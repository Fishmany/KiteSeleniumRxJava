import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class ApplicationController {
  private static ApplicationController applicationController;
  private final PriorityBlockingQueue<StockMessage> query;
  private final Subject<Pair<String, String>> result;
  private static final double target = 0.01;
  private static final double stopLoss = 0.005;

  private ApplicationController() {
    query = new PriorityBlockingQueue<>(10, new StockMessageComparator());
    result = BehaviorSubject.create();
  }

  public static ApplicationController getInstance() {
    if (applicationController == null) applicationController = new ApplicationController();
    return applicationController;
  }

  public static void main(String[] args) {
    ApplicationController controller = ApplicationController.getInstance();
    WebAction.getInstance().login();

    new Thread(
            () -> {
              for (int i = 2; i <= 6; i++) {
                controller.query.add(new StockMessage(Integer.MIN_VALUE, "ClickMarketWatch", new Pair<String,String>("",""+i)));
                controller.query.add(new StockMessage(Integer.MIN_VALUE, "ReadStockValue", null));
                WebAction.sleep(100);
              }
            })
        .start();

    new Thread(
            () -> {
              StockMessage message = controller.query.poll();
              switch (message.getMessage()) {
                case "ClickMarketWatch":
                  WebAction.getInstance().clickMarketWatch(message.getPair().getValue());
                  break;
                case "ReadStockValue":
                  WebAction.getInstance().readStockPrice();
                  break;
                case "BUY":
                  String buyPrice = message.getPair().getValue();
                  String buyName = message.getPair().getKey();
                  String i = StockLocation.getPosition(buyName);
                  String buyTarget = controller.getTarget(buyPrice);
                  String buyStopLoss = controller.getStopLoss(buyPrice);
                  WebAction.getInstance().clickMarketWatch(StockLocation.getMarketWatch(buyName));
                  WebAction.getInstance()
                      .buySellBO(i, "1", buyPrice, buyTarget, buyStopLoss, "1", true);
                  break;
                case "SELL":
                  String sellPrice = message.getPair().getValue();
                  String sellName = message.getPair().getKey();
                  String j = StockLocation.getPosition(sellName);
                  String sellTarget = controller.getTarget(sellPrice);
                  String sellStopLoss = controller.getStopLoss(sellPrice);
                  WebAction.getInstance().clickMarketWatch(StockLocation.getMarketWatch(sellName));
                  WebAction.getInstance()
                      .buySellBO(j, "1", sellPrice, sellTarget, sellStopLoss, "1", false);
                  break;
              }
            })
        .start();
  }

  private String getTarget(String price) {
    return "" + Double.parseDouble(price) * target;
  }

  private String getStopLoss(String price) {
    return "" + Double.parseDouble(price) * stopLoss;
  }

  public PriorityBlockingQueue<StockMessage> getQuery() {
    return query;
  }

  public Subject<Pair<String, String>> getResult() {
    return result;
  }
}

class StockMessageComparator implements Comparator<StockMessage> {

  @Override
  public int compare(StockMessage o1, StockMessage o2) {
    return o1.getPriority() - o2.getPriority();
  }
}
