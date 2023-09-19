package bloomingbit.io.infoproducer.job;

/**
 * packageName: bloomingbit.io.infoproducer.job
 * fileName: JobRegistration
 * author: park.jonghyuk
 * date: 2023/09/19
 * description
 * ===========================================================
 * DATE         AUTHOR          NOTE
 * -----------------------------------------------------------
 * 2023/09/19      park.jonghyuk         최초 생성
 * ===========================================================
 */

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.lite.api.bootstrap.impl.ScheduleJobBootstrap;
import org.apache.shardingsphere.elasticjob.lite.internal.storage.JobNodePath;
import org.apache.shardingsphere.elasticjob.lite.internal.storage.JobNodeStorage;
import org.apache.shardingsphere.elasticjob.lite.lifecycle.api.JobOperateAPI;
import org.apache.shardingsphere.elasticjob.lite.lifecycle.internal.operate.JobOperateAPIImpl;
import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import lombok.extern.slf4j.Slf4j;

/**
 * repository 에서 spec 읽어서, elasticJob 으로 등록한다.( schedule !)
 */
@Slf4j
@Component
public class JobRegistration {

  /** 등록할 job name 에 붙을 prefix */
  private static final String JOB_PREFIX = "bloomingbit-bot-";

  @Value("${spring.profiles.active}")
  private String activeProfile;

  /**
   * job 을 managing 할 수 있는 API 를 제공하는 객체 <p>
   * elastic-job 에 속해있다. 여기서는 shutdown 기능만 사용한다.
   */
  private JobOperateAPI jobOperateAPI;

  /** repository 등등을 wrapping 한 facade... */
  private DataManager          dataManager;

  /** elastic job 사용할 때 필요한 job registration center */
  private CoordinatorRegistryCenter elasticJobRegCenter ;

  /** slack 채널로 alert 하기 위한 객체 */
  private SlackService                slackBot;


  public JobRegistration(
          DataManager          dataMgr,
          CoordinatorRegistryCenter elasticJobRegCenter,
          SlackService slackBot
  ){
    this.dataManager         = dataMgr;
    this.elasticJobRegCenter = elasticJobRegCenter;
    this.slackBot        = slackBot;
    this.jobOperateAPI       = new JobOperateAPIImpl( elasticJobRegCenter );
  }

  /**
   * init 하면서 spec list 를 모두 받아서 job 을 등록한다.
   */
  @PostConstruct
  public void initialize(){
    List<ImplSpec> allSpecList = dataManager.getAllSpec();

    log.info("--------------------- spec count: {}", allSpecList.size());

    for( JobSpec spec : allSpecList ){
      if ( spec.isExecuteFlag()) {
        registJob(spec);
      }
    }
  }

  /**
   * init job 등록 이후, 추가되는 job 들은 중간에 치고 들어가는 경우이다. <p>
   * thread 간 경쟁이 심하게 생길 경우가 아니므로, 함수 level 에서  synchronized <p>
   * */
  public synchronized void registJob( JobSpec spec ) {
    if (spec.isExecuteFlag()) {
      if ( spec instanceof ImplSpec ) {
        setupJobs( elasticJobRegCenter, (ImplSpec) spec );
      }
    }
  }

  /**
   * registJob 과 마찬가지로 함수 level synchronized
   * @throws NullPointerException
   *          zk 에서 job 을 찾지 못할 때 발생할 수 있다.
   */
  public synchronized void dereigstJob( JobSpec spec ) {
    int valueId = spec.getId();
    String jobName = toJobName( valueId );
    String pathKey = toJobPath( jobName );

    // 일단 해당 spec 의 job이 등록되어 있는지 확인
    if ( elasticJobRegCenter.isExisted(pathKey)) {
      // Disabling a job will cause other distributed jobs to trigger resharding.
      // disable 은 re-shard 하도록 한다. 즉, 여기서 원하는 동작이 아니다.
      // jobOperateAPI.disable(jobName, null);
      jobOperateAPI.shutdown(jobName, null); // shutdown, remove 둘 다 된다. -> shutdown 을 하면 ui 에는 crushed 상태
      elasticJobRegCenter.remove(pathKey); // job reg center 에서도 지운다.
    }
  }

  /**
   * Job 을 elasticJob 에 등록하고, schedule 한다. <p/>
   * DB 에 등록된 spec 정보를 가져와서, ElasticJob 에 등록한뒤, spec 에 있는 cron expression 으로 schedule 한다.
   * @param elasticJobRegCenter
   * @param spec
   */
  private void setupJobs(CoordinatorRegistryCenter elasticJobRegCenter, ImplSpec spec){
    // elasticjob-lite-spring-boot-starter 를 사용하면, configuration 파일에서, scheduling 을 할 수 있지만,
    // 해당 project 는 DB 에서, schedule 할 job 정보를 가져오므로( schedule cron expression 까지),
    // spring-boot 용 library 를 사용하지 않는다

    String jobName = toJobName( spec.getJobName() );


    // RX 등록
    new ScheduleJobBootstrap(
            elasticJobRegCenter
            , new ImpleTask(spec, dataManager, slackBot )
            , JobConfiguration.newBuilder( jobName, 1 ) // shard 는 1개,
            .cron( spec.getCronExpression() )
            .timeZone("GMT+0") // 위의 spec.getCronExpression() 으로 나오는 시간대는 UTC 를 기준으로 함.
            // elasticJob 이 사용하는 scheduler 인 quartz 는 "GMT" 로 시작하는 timezone string 을 요구한다.
            .jobShardingStrategyType( "{SHARDING_STRATEGY}")
            .overwrite( true )
            .build()).schedule();

  }

  private String toJobName( String mnemonic ) {
    return JOB_PREFIX + mnemonic;
  }

  private String toJobName( int id ) {
    String mnemonic = dataManager.getValueName(id);
    return toJobName(mnemonic);
  }

  /**
   * "/jobName" 형태를 return 한다. <p>
   * 이 jobPath 의 형식은 elastic-job 의 정책에 달려있다. (dependency!!!) <p>
   * 하지만 이 형식의 값을 return 받을 수 있는 함수는 없다. <p>
   *
   * *** => elastic-job 의 정책이 바뀌면 여기서 수정한다 *** <p>
   *
   * ***************************************************** <p>
   * {@link JobNodeStorage}, {@link JobNodePath} 는 해당 job 하위의 node 들에 대한 작업이다. <p>
   * job 자체를 다루는건 "/jobName", "jobName" 두가지로만 가능했다.) <p>
   * *****************************************************<p>
   * @param jobName
   * @return
   */
  private String toJobPath( String jobName) {
    return "/" + jobName;
  }
}