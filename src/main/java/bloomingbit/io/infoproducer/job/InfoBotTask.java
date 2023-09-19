package bloomingbit.io.infoproducer.job;

import org.apache.shardingsphere.elasticjob.simple.job.SimpleJob;

/**
 * packageName: bloomingbit.io.infoproducer.job
 * fileName: InfoBotTask
 * author: park.jonghyuk
 * date: 2023/09/19
 * description
 * ===========================================================
 * DATE         AUTHOR          NOTE
 * -----------------------------------------------------------
 * 2023/09/19      park.jonghyuk         최초 생성
 * ===========================================================
 */

public interface InfoBotTask extends SimpleJob {

    void initialize();

    void crawling();
}
