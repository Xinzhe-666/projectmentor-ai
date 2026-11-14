package example.orders;

public final class OrderService {
    private final OrderRepository repository;
    public OrderService(OrderRepository repository) { this.repository = repository; }
    public void cancelOrder(long orderId) { repository.markCancelled(orderId); }
}
