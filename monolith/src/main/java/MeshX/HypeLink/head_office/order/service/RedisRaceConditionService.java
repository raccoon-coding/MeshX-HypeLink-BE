package MeshX.HypeLink.head_office.order.service;

import MeshX.HypeLink.common.TryCatchTemplate;
import MeshX.HypeLink.head_office.item.model.entity.ItemDetail;
import MeshX.HypeLink.head_office.item.repository.ItemDetailJpaRepositoryVerify;
import MeshX.HypeLink.head_office.order.exception.PurchaseOrderException;
import MeshX.HypeLink.head_office.order.model.dto.request.PurchaseOrderCreateReq;
import MeshX.HypeLink.head_office.order.model.entity.PurchaseOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static MeshX.HypeLink.head_office.order.exception.PurchaseOrderExceptionMessage.UNDER_ZERO;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RedisRaceConditionService {
    private final RedissonClient redissonClient;

    private final ItemDetailJpaRepositoryVerify itemDetailRepository;

    @Transactional
    public void decreaseHeadItemDetailStock(PurchaseOrderCreateReq dto) throws PurchaseOrderException {
        RLock lock = getRLockWithItemDetailCode(dto.getItemDetailCode());

        TryCatchTemplate.withLock(lock, 3, 10, () -> {
            ItemDetail itemDetail = itemDetailRepository.findByItemDetailCode(dto.getItemDetailCode());
            if(itemDetail.getStock() - dto.getQuantity() < 0) {
                throw new PurchaseOrderException(UNDER_ZERO);
            }
            itemDetail.updateStock(-1 * dto.getQuantity());
            itemDetailRepository.merge(itemDetail);
            log.info("재고 차감 완료 (남은 재고: {})", itemDetail.getStock());
        }, e -> {
            log.error("재고 차감 실패 - {}", e.getMessage(), e);
            throw new PurchaseOrderException(UNDER_ZERO);
        });
    }

    @Transactional
    public void increaseHeadItemDetailStock(PurchaseOrder purchaseOrder) throws PurchaseOrderException {
        RLock lock = getRLockWithItemDetailCode(purchaseOrder.getItemDetail().getItemDetailCode());

        TryCatchTemplate.withLock(lock, 3, 10, () -> {
            ItemDetail itemDetail = itemDetailRepository.findByItemDetailCode(purchaseOrder.getItemDetail().getItemDetailCode());
            itemDetail.updateStock(purchaseOrder.getQuantity());
            if(itemDetail.getStock() < 0) {
                throw new PurchaseOrderException(UNDER_ZERO);
            }
            itemDetailRepository.merge(itemDetail);
            log.info("재고 완료 (남은 재고: {})", itemDetail.getStock());
        }, e -> {
            log.error("재고 차감 실패 - {}", e.getMessage(), e);
            throw new PurchaseOrderException(UNDER_ZERO);
        });
    }

    private RLock getRLockWithItemDetailCode(String itemDetailCode) {
        String lockKey = "lock:item_code:" + itemDetailCode;
        return redissonClient.getLock(lockKey);
    }
}
