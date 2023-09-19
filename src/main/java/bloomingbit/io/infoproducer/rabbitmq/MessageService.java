package bloomingbit.io.infoproducer.rabbitmq;

import bloomingbit.io.infoproducer.rabbitmq.model.PriceChangeInfo;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * packageName: bloomingbit.io.infoproducer.rabbitmq
 * fileName: MessageService
 * author: park.jonghyuk
 * date: 2023/09/18
 * description
 * ===========================================================
 * DATE         AUTHOR          NOTE
 * -----------------------------------------------------------
 * 2023/09/18      park.jonghyuk         최초 생성
 * ===========================================================
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageService {


  @Value("${rabbitmq.exchange.name}")
  private String exchangeName;

  @Value("${rabbitmq.routing.key}")
  private String routingKey;

  private final RabbitTemplate rabbitTemplate;

  /**
   * Queue로 메시지를 발행
   *
   * @param priceChangeInfo 발행할 메시지의 DTO 객체
   */
  public void sendMessage(PriceChangeInfo priceChangeInfo) {
    log.info("message sent: {}", priceChangeInfo.toString());
    rabbitTemplate.convertAndSend(exchangeName, routingKey, priceChangeInfo);
  }

  /**
   * Queue에서 메시지를 구독
   *
   * @param priceChangeInfo 구독한 메시지를 담고 있는 PriceChangeInfo 객체
   */
  @RabbitListener(queues = "${rabbitmq.queue.name}")
  public void reciveMessage(PriceChangeInfo priceChangeInfo) {
    log.info("Received message: {}", priceChangeInfo.toString());
  }
}