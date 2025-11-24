package MeshX.HypeLink.head_office.item.repository;

import MeshX.HypeLink.head_office.item.model.entity.Item;
import MeshX.HypeLink.head_office.item.model.entity.ItemDetail;
import MeshX.HypeLink.head_office.order.model.dto.response.PurchaseOrderInfoRes;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemDetailRepository extends JpaRepository<ItemDetail, Integer> {
    Optional<ItemDetail> findByItemDetailCode(String itemDetailCode);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "javax.persistence.lock.timeout", value = "5000")
    })
    @Query("""
    SELECT id 
    FROM ItemDetail id 
    LEFT JOIN FETCH id.item it 
    WHERE id.itemDetailCode = :itemDetailCode
    """)
    Optional<ItemDetail> findByItemDetailCodeForUpdateWithLock(@Param("itemDetailCode") String itemDetailCode);

    @Query("""
    select distinct itemD
    from ItemDetail itemD
    left join fetch itemD.item i
    left join fetch i.category ca
    left join fetch i.itemImages img
    left join fetch img.image im
    left join fetch itemD.color c
    left join fetch itemD.size s
    where itemD.item = :item
    """)
    List<ItemDetail> findByItem(Item item);
    @Query("SELECT d FROM ItemDetail d JOIN FETCH d.item i WHERE i.id = :id")
    List<ItemDetail> findByItemId(Integer id);

    // 모든 ItemDetail 조회 시 Item도 함께 Fetch
    @Query("SELECT DISTINCT d FROM ItemDetail d JOIN FETCH d.item")
    List<ItemDetail> findAllWithItem();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "javax.persistence.lock.timeout", value = "5000") // 5초 대기 후 예외 발생
    })
    @Query("SELECT d FROM ItemDetail d WHERE d.id = :id")
    Optional<ItemDetail> findByIdForUpdateWithLock(@Param("id") Integer id); // 비관적 락을 적용한 상태

    @Query(
            value = """
        SELECT new MeshX.HypeLink.head_office.order.model.dto.response.PurchaseOrderInfoRes(
            d.id,
            i.koName,
            i.enName,
            c.category,
            col.colorName,
            col.colorCode,
            i.itemCode,
            d.itemDetailCode,
            d.stock,
            CAST(COALESCE(SUM(
                CASE WHEN p.purchaseOrderState = MeshX.HypeLink.head_office.order.model.entity.PurchaseOrderState.REQUESTED
                     THEN p.quantity ELSE 0 END
            ), 0) AS integer)
        )
        FROM ItemDetail d
        LEFT JOIN d.item i
        LEFT JOIN i.category c
        LEFT JOIN d.color col
        LEFT JOIN PurchaseOrder p ON p.itemDetail = d
        GROUP BY i.id, i.koName, i.enName, c.category, col.colorName, col.colorCode,
                 i.itemCode, d.itemDetailCode, d.stock
        """,
            countQuery = """
        SELECT COUNT(d)
        FROM ItemDetail d
    """
    )
    Page<PurchaseOrderInfoRes> findItemDetailWithRequestedTotalQuantity(Pageable pageable);

    @Modifying
    @Query(value = """
INSERT INTO item_detail (
  id,
  color_id,
  size_id,
  item_detail_code,
  stock,
  item_id,
  created_at,
  updated_at
)
VALUES (
  :#{#entity.id},
  :#{#entity.color.id},
  :#{#entity.size.id},
  :#{#entity.itemDetailCode},
  :#{#entity.stock},
  :#{#entity.item.id},
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE
  color_id = VALUES(color_id),
  size_id = VALUES(size_id),
  item_detail_code = VALUES(item_detail_code),
  stock = VALUES(stock),
  item_id = VALUES(item_id),
  updated_at = NOW()
""", nativeQuery = true)
    void upsert(@Param("entity") ItemDetail entity);
}
