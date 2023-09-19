package bloomingbit.io.infoproducer.rabbitmq.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * packageName: bloomingbit.io.infoproducer.rabbitmq.model
 * fileName: priceChangeInfo
 * author: park.jonghyuk
 * date: 2023/09/18
 * description
 * ===========================================================
 * DATE         AUTHOR          NOTE
 * -----------------------------------------------------------
 * 2023/09/18      park.jonghyuk         최초 생성
 * ===========================================================
 */

@Setter
@Getter
@ToString
public class PriceChangeInfo {

  private BigDecimal currPrice;
  private BigDecimal prevPrice;
  private BigDecimal changePercent;


}
