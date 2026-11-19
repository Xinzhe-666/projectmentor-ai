package example.orders;

public interface OrderRepository {
    void markCancelled(long orderId);
}
