package bloomingbit.io.infoproducer.controller;

import bloomingbit.io.infoproducer.rabbitmq.MessageService;
import bloomingbit.io.infoproducer.rabbitmq.model.PriceChangeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * packageName: bloomingbit.io.infoproducer.controller
 * fileName: MessageController
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
@RequestMapping("/")
@RestController

public class MessageController {

  private final MessageService messageService;

  @PostMapping("/send/message")
  public ResponseEntity<?> sendMessage(@RequestBody PriceChangeInfo info) {
    messageService.sendMessage(info);
    return ResponseEntity.ok("Message sent to RabbitMQ!");
  }



}
