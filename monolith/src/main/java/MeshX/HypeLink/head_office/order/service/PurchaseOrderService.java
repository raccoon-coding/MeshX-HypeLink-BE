package MeshX.HypeLink.head_office.order.service;

import MeshX.HypeLink.auth.model.entity.Member;
import MeshX.HypeLink.auth.model.entity.Store;
import MeshX.HypeLink.auth.repository.MemberJpaRepositoryVerify;
import MeshX.HypeLink.auth.repository.StoreJpaRepositoryVerify;
import MeshX.HypeLink.auth.utils.JwtUtils;
import MeshX.HypeLink.common.BaseResponse;
import MeshX.HypeLink.common.Page.PageRes;
import MeshX.HypeLink.common.TryCatchTemplate;
import MeshX.HypeLink.common.exception.BaseException;
import MeshX.HypeLink.direct_store.item.model.entity.StoreItem;
import MeshX.HypeLink.direct_store.item.model.entity.StoreItemDetail;
import MeshX.HypeLink.direct_store.item.repository.StoreItemDetailJpaRepositoryVerify;
import MeshX.HypeLink.direct_store.item.repository.StoreItemJpaRepositoryVerify;
import MeshX.HypeLink.head_office.item.model.entity.ItemDetail;
import MeshX.HypeLink.head_office.item.repository.ItemDetailJpaRepositoryVerify;
import MeshX.HypeLink.head_office.order.exception.PurchaseOrderException;
import MeshX.HypeLink.head_office.order.model.dto.request.HeadPurchaseOrderCreateReq;
import MeshX.HypeLink.head_office.order.model.dto.request.PurchaseOrderCreateReq;
import MeshX.HypeLink.head_office.order.model.dto.request.PurchaseOrderUpdateReq;
import MeshX.HypeLink.head_office.order.model.dto.response.*;
import MeshX.HypeLink.head_office.order.model.entity.PurchaseDetailStatus;
import MeshX.HypeLink.head_office.order.model.entity.PurchaseOrder;
import MeshX.HypeLink.head_office.order.model.entity.PurchaseOrderState;
import MeshX.HypeLink.head_office.order.repository.PurchaseOrderJpaRepositoryVerify;
import MeshX.HypeLink.head_office.order.service.dto.ItemDetailUpdateDto;
import MeshX.HypeLink.head_office.order.service.dto.UpdateStoreItemDetailReq;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static MeshX.HypeLink.auth.model.entity.MemberRole.*;
import static MeshX.HypeLink.head_office.order.exception.PurchaseOrderExceptionMessage.UNDER_ZERO;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseOrderService {
    private static final int MAX_RETRY = 3;
    @Value("${store.direct.uri}")
    private String directServerUri;

    private final KafkaPurchaseService kafkaPurchaseService;
    private final RedisRaceConditionService redisRaceConditionService;

    private final PurchaseOrderJpaRepositoryVerify orderRepository;
    private final ItemDetailJpaRepositoryVerify itemDetailRepository;
    private final StoreItemJpaRepositoryVerify storeItemRepository;
    private final StoreItemDetailJpaRepositoryVerify storeItemDetailRepository;
    private final MemberJpaRepositoryVerify memberRepository;
    private final StoreJpaRepositoryVerify storeRepository;
    private final JwtUtils jwtUtils;

    @Transactional
    public void createHeadOrder(HeadPurchaseOrderCreateReq dto) {
        ItemDetail itemDetail = itemDetailRepository.findById(dto.getItemDetailId());
        Member requester = memberRepository.findByEmail("hq@company.com");
        Member supplier = memberRepository.findByEmail("hq@company.com");

        if(itemDetail.getStock() + dto.getQuantity() < 0) {
            throw new PurchaseOrderException(UNDER_ZERO);
        }

        orderRepository.createOrder(dto.toEntity(itemDetail, requester, supplier));
    }

    @Transactional
    public void createOrder(PurchaseOrderCreateReq dto) {
        Store requesterStore = storeRepository.findById(dto.getRequestStoreId());
        Member requester = requesterStore.getMember();
        Member supplier;
        ItemDetail itemDetail = itemDetailRepository.findByItemDetailCodeWithLock(dto.getItemDetailCode());

        if(dto.getSupplierStoreId() == 0) {
            // Redis 이용 Lock
            redisRaceConditionService.decreaseHeadItemDetailStock(dto);
            supplier = memberRepository.findByEmail("hq@company.com");
//            if(itemDetail.getStock() - dto.getQuantity() < 0) {
//                throw new PurchaseOrderException(UNDER_ZERO);
//            }
//            itemDetail.updateStock(-1 * dto.getQuantity());
//            kafkaPurchaseService.syncItemDetailStockMinus(itemDetail, dto.getQuantity());
        } else {
            String itemCode = itemDetail.getItem().getItemCode();
            supplier = verifyStoreItemStock(dto, itemCode);
            Store store = storeRepository.findByMember(supplier);
            updateStock(store, itemCode, dto.getItemDetailCode(), -1 * dto.getQuantity());
            updateStoreItemStock(store, dto.getItemDetailCode(), dto.getItemDetailCode(), -1 * dto.getQuantity());
//            kafkaPurchaseService.syncDirectItemDetailStock(store, itemDetail.getId(), -1 * dto.getQuantity());
        }

        orderRepository.createOrder(dto.toEntity(itemDetail, requester, supplier));
    }

    private Member verifyStoreItemStock(PurchaseOrderCreateReq dto, String itemCode) {
        Store supplierStore = storeRepository.findById(dto.getSupplierStoreId());
        Member supplier = supplierStore.getMember();
        String at = jwtUtils.createAccessToken("hq@company.com", "ADMIN");
        // --- HTTP GET 호출 ---
        WebClient webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + at)
                .baseUrl(directServerUri) // 실제 서버 주소로 변경
                .build();

        BaseResponse<ItemDetailUpdateDto> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/direct/store/item/detail/code")
                        .queryParam("itemDetailCode", dto.getItemDetailCode())
                        .queryParam("storeId", dto.getSupplierStoreId())
                        .queryParam("itemCode", itemCode)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<ItemDetailUpdateDto>>() {})
                .block(); // 동기식 호출 (트랜잭션 내에서 즉시 대기)

        if(response == null) {
            throw new BaseException(null);
        }
        if(response.getData().getStock() < dto.getQuantity()) {
            throw new BaseException(null);
        }
        return supplier;
    }

    public PurchaseOrderInfoDetailRes getDetails(Integer id) {
        PurchaseOrder PurchaseOrder = orderRepository.findById(id);
        return PurchaseOrderInfoDetailRes.toDto(PurchaseOrder);
    }

    public PurchaseOrderInfoDetailListRes getList() {
        List<PurchaseOrder> purchaseOrders = orderRepository.findAll();
        return PurchaseOrderInfoDetailListRes.toDto(purchaseOrders);
    }

    public PageRes<PurchaseOrderInfoDetailRes> searchList(Pageable pageReq, String keyWord, String status,
                                                          String email) {
        Member member = memberRepository.findByEmail(email);

        if(member.getRole().equals(BRANCH_MANAGER)) {
            Page<PurchaseOrder> orderPage = orderRepository.findSearchByRequester(keyWord, status, member, pageReq);
            Page<PurchaseOrderInfoDetailRes> dtoPage = PurchaseOrderInfoDetailRes.toDtoPage(orderPage);
            return PageRes.toDto(dtoPage);
        } else if (member.getRole().equals(ADMIN) || member.getRole().equals(MANAGER)) {
            Page<PurchaseOrder> orders = orderRepository.findAllSearchOrderWithPriority(keyWord, status, pageReq);
            Page<PurchaseOrderInfoDetailRes> dtoPage = PurchaseOrderInfoDetailRes.toDtoPage(orders);
            return PageRes.toDto(dtoPage);
        }
        throw new BaseException(null);
    }

    public PageRes<PurchaseOrderInfoRes> getPurchaseOrderList(Pageable pageReq, String keyWord, String category) {
        Page<PurchaseOrderInfoRes> page = itemDetailRepository.findItemsAndPurchaseOrdersWithPaging(pageReq, keyWord, category);
        return PageRes.toDto(page);
    }

    public PurchaseOrderStateInfoListRes getPurchaseStateList() {
        List<PurchaseOrderStateInfoRes> states = Arrays.stream(PurchaseOrderState.values())
                .map(state -> PurchaseOrderStateInfoRes.builder()
                        .description(state.getDescription())
                        .build())
                .collect(Collectors.toList());

        return PurchaseOrderStateInfoListRes.builder()
                .purchaseOrderStates(states)
                .build();
    }

    public PurchaseDetailsStatusInfoListRes getPurchaseDetailStatuses() {
        List<PurchaseDetailsStatusInfoRes> states = Arrays.stream(PurchaseDetailStatus.values())
                .map(state -> PurchaseDetailsStatusInfoRes.builder()
                        .description(state.getDescription())
                        .build())
                .collect(Collectors.toList());

        return PurchaseDetailsStatusInfoListRes.builder()
                .purchaseDetailsStatusInfos(states)
                .build();
    }

    // Redis 이용 Lock
    @Transactional
    public PurchaseOrderInfoDetailRes update(PurchaseOrderUpdateReq dto) {
        PurchaseOrder purchaseOrder = orderRepository.findById(dto.getOrderId());
        processOrderUpdate(dto);

        PurchaseOrderState state = PurchaseOrderState.valueOf(dto.getOrderState());
        purchaseOrder.updateOrderState(state);

        PurchaseOrder update = orderRepository.update(purchaseOrder);
        return PurchaseOrderInfoDetailRes.toDto(update);
    }

    // 비관적 락을 못 얻었을 때 재시도 로직
//    @Transactional(rollbackFor = Exception.class)
//    public PurchaseOrderInfoDetailRes update(PurchaseOrderUpdateReq dto) {
//        int attempt = 0;
//
//        while (true) {
//            try {
//                return processOrderUpdate(dto); // 핵심 처리 메서드
//            } catch (LockTimeoutException | PessimisticLockException e) {
//                attempt++;
//                if (attempt >= MAX_RETRY) {
//                    throw new BaseException(null); // 커스텀 예외
//                }
//                try {
//                    Thread.sleep(200L * attempt); // 점진적 backoff
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    throw new BaseException(null);
//                }
//            }
//        }
//    }

    // 비관적 락으로 업데이트 로직
    private PurchaseOrderInfoDetailRes processOrderUpdate(PurchaseOrderUpdateReq dto) {
        PurchaseOrder purchaseOrder = orderRepository.findById(dto.getOrderId());
        Member headMember = memberRepository.findByEmail("hq@company.com");
        PurchaseOrderState state = PurchaseOrderState.valueOf(dto.getOrderState());

        Store store = storeRepository.findByMember(purchaseOrder.getRequester());
        String itemCode = purchaseOrder.getItemDetail().getItem().getItemCode();
        Integer itemDetailId = purchaseOrder.getItemDetail().getId();
        String itemDetailCode = purchaseOrder.getItemDetail().getItemDetailCode();
        Integer quantity = purchaseOrder.getQuantity();

        if(state.equals(PurchaseOrderState.COMPLETED) && purchaseOrder.getPurchaseOrderState().equals(PurchaseOrderState.REQUESTED)) {
            if(purchaseOrder.getRequester().equals(headMember)) {
                ItemDetail itemDetail = itemDetailRepository.findById(purchaseOrder.getItemDetail().getId());
//                itemDetail.updateStock(purchaseOrder.getQuantity());
//                if(itemDetail.getStock() < 0) {
//                    throw new PurchaseOrderException(UNDER_ZERO);
//                }
//                itemDetailRepository.merge(itemDetail);
                redisRaceConditionService.increaseHeadItemDetailStock(purchaseOrder);
                kafkaPurchaseService.syncItemDetailStock(itemDetail, purchaseOrder.getQuantity());
            } else {
                updateStock(store, itemCode, itemDetailCode, quantity);

                updateStoreItemStock(store, itemCode, itemDetailCode, quantity);
//                kafkaPurchaseService.syncDirectItemDetailStock(store, itemDetailId, quantity);
            }
        } else {
            if(purchaseOrder.getPurchaseOrderState().equals(PurchaseOrderState.COMPLETED)) {
                return PurchaseOrderInfoDetailRes.toDto(purchaseOrder);
            } else {
                purchaseOrder.updateOrderDetailStatus(PurchaseDetailStatus.OTHER_CANCELLATION);
                Store supplier = storeRepository.findByMember(purchaseOrder.getSupplier());
                updateStock(supplier, itemCode, itemDetailCode, quantity);
                updateStoreItemStock(supplier, itemCode, itemDetailCode, quantity);
            }
        }

        purchaseOrder.updateOrderState(state);

        PurchaseOrder update = orderRepository.update(purchaseOrder);
        return PurchaseOrderInfoDetailRes.toDto(update);
    }

    private void updateStock(Store store, String itemCode, String itemDetailCode, Integer quantity) {
        StoreItem storeItem = storeItemRepository.findByStoreAndItemCode(store, itemCode);
        StoreItemDetail storeItemDetail = storeItemDetailRepository.findByStoreItemAndItemDetailCode(storeItem, itemDetailCode);

        storeItemDetail.updateStock(quantity);

        storeItemDetailRepository.mergeItemDetail(storeItemDetail);
    }

    private void updateStoreItemStock(Store store, String itemCode, String itemDetailCode, Integer quantity) {
        String at = jwtUtils.createAccessToken("hq@company.com", "ADMIN");

        // --- HTTP Patch 호출 ---
        WebClient webClient = WebClient.builder()
                .baseUrl(directServerUri) // 실제 서버 주소로 변경
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + at)
                .build();

        BaseResponse<String> response = webClient.patch()
                .uri("/api/direct/store/item/detail/update")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateStoreItemDetailReq.builder()
                        .storeId(store.getId())
                        .itemCode(itemCode)
                        .itemDetailCode(itemDetailCode)
                        .updateStock(quantity)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<String>>() {})
                .block(); // 동기식 호출

        if (response == null || response.getData() == null) {
            throw new BaseException(null);
        }
    }
}
