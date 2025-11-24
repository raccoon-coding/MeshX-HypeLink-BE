package com.example.apiauth.usecase.service;

import MeshX.common.BaseResponse;
import MeshX.common.UseCase;
import com.example.apiauth.adapter.in.web.dto.StoreInfoResDto;
import com.example.apiauth.adapter.in.web.dto.StoreStateReqDto;
import com.example.apiauth.adapter.in.web.dto.UserReadResDto;
import com.example.apiauth.adapter.out.geocoding.dto.GeocodeDto;
import com.example.apiauth.adapter.out.external.shipment.ShipmentClient;
import com.example.apiauth.common.exception.AuthException;
import com.example.apiauth.domain.model.Driver;
import com.example.apiauth.domain.model.Member;
import com.example.apiauth.domain.model.Pos;
import com.example.apiauth.domain.model.Store;
import com.example.apiauth.domain.model.value.MemberRole;
import com.example.apiauth.usecase.port.out.external.GeocodingPort;
import com.example.apiauth.usecase.port.out.persistence.DriverCommandPort;
import com.example.apiauth.usecase.port.out.persistence.DriverQueryPort;
import com.example.apiauth.usecase.port.out.persistence.MemberCommandPort;
import com.example.apiauth.usecase.port.out.persistence.MemberQueryPort;
import com.example.apiauth.usecase.port.out.persistence.PosCommandPort;
import com.example.apiauth.usecase.port.out.persistence.PosQueryPort;
import com.example.apiauth.usecase.port.out.persistence.StoreCommandPort;
import com.example.apiauth.usecase.port.out.persistence.StoreQueryPort;
import com.example.apiauth.usecase.port.out.usecase.MemberCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.apiauth.common.exception.AuthExceptionMessage.*;

@UseCase
@RequiredArgsConstructor
public class MemberCommandService implements MemberCommandUseCase {

    private final MemberCommandPort memberCommandPort;
    private final MemberQueryPort memberQueryPort;
    private final StoreCommandPort storeCommandPort;
    private final StoreQueryPort storeQueryPort;
    private final PosCommandPort posCommandPort;
    private final PosQueryPort posQueryPort;
    private final DriverCommandPort driverCommandPort;
    private final DriverQueryPort driverQueryPort;
    private final GeocodingPort geocodingPort;
    private final ShipmentClient shipmentClient;

    @Override
    @Transactional
    public void updateStoreInfo(Integer id, StoreInfoResDto dto) {
        Store store = storeQueryPort.findById(id);
        Member member = store.getMember();

        // Member 정보 업데이트
        Member updatedMember = Member.builder()
                .id(member.getId())
                .email(member.getEmail())
                .password(member.getPassword())
                .name(dto.getName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .role(member.getRole())
                .region(dto.getRegion())
                .build();

        memberCommandPort.save(updatedMember);

        // Geocoding으로 좌표 조회
        GeocodeDto geocodeDto = geocodingPort.getCoordinates(dto.getAddress());

        // Store 정보 업데이트
        Store updatedStore = Store.builder()
                .id(store.getId())
                .member(updatedMember)
                .lat(geocodeDto.getLatAsDouble())
                .lon(geocodeDto.getLonAsDouble())
                .storeNumber(dto.getStoreNumber())
                .posCount(store.getPosCount())
                .storeState(store.getStoreState())
                .build();

        storeCommandPort.save(updatedStore);
    }

    @Override
    @Transactional
    public void storeStateUpdate(Integer id, StoreStateReqDto dto) {
        Store store = storeQueryPort.findById(id);

        Store updatedStore = Store.builder()
                .id(store.getId())
                .member(store.getMember())
                .lat(store.getLat())
                .lon(store.getLon())
                .storeNumber(store.getStoreNumber())
                .posCount(store.getPosCount())
                .storeState(dto.getStoreState())
                .syncStatus(store.getSyncStatus())
                .build();

        storeCommandPort.save(updatedStore);
    }

    @Override
    @Transactional
    public void updateUser(Integer id, UserReadResDto dto) {
        Member member = memberQueryPort.findById(id);

        if (!id.equals(dto.getId())) {
            throw new AuthException(ID_MISMATCH);
        }

        // Member 정보 업데이트
        Member updatedMember = Member.builder()
                .id(member.getId())
                .email(member.getEmail())
                .password(member.getPassword())
                .name(dto.getName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .role(member.getRole())
                .region(dto.getRegion())
                .build();

        memberCommandPort.save(updatedMember);

        // Role에 따른 추가 업데이트
        if (member.getRole() == MemberRole.DRIVER) {
            Driver driver = driverQueryPort.findByMember_Id(member.getId());
            Driver updatedDriver = Driver.builder()
                    .id(driver.getId())
                    .member(updatedMember)
                    .macAddress(dto.getMacAddress())
                    .carNumber(driver.getCarNumber())
                    .build();
            driverCommandPort.save(updatedDriver);
        }

        if (member.getRole() == MemberRole.BRANCH_MANAGER) {
            Store store = storeQueryPort.findByMemberId(member.getId());

            GeocodeDto geocodeDto = geocodingPort.getCoordinates(dto.getAddress());

            Store updatedStore = Store.builder()
                    .id(store.getId())
                    .member(updatedMember)
                    .lat(geocodeDto.getLatAsDouble())
                    .lon(geocodeDto.getLonAsDouble())
                    .storeNumber(dto.getStoreNumber())
                    .posCount(store.getPosCount())
                    .storeState(store.getStoreState())
                    .build();

            storeCommandPort.save(updatedStore);
        }
    }

    @Override
    @Transactional
    public void deleteUser(Integer id) {
        Member member = memberQueryPort.findById(id);

        switch (member.getRole()) {
            case BRANCH_MANAGER:
                Store store = storeQueryPort.findByMemberId(member.getId());
                List<Pos> posDevices = posQueryPort.findByStoreIdIn(List.of(store.getId()));
                if (!posDevices.isEmpty()) {
                    throw new AuthException(CANNOT_DELETE_STORE_WITH_POS);
                }
                storeCommandPort.delete(store.getId());
                break;

            case POS_MEMBER:
                Pos pos = posQueryPort.findByMember_Id(member.getId());
                Store posStore = pos.getStore();

                // posCount 감소
                Store decreasedStore = posStore.decreasePosCount();
                storeCommandPort.save(decreasedStore);

                posCommandPort.delete(pos.getId());
                break;

            case DRIVER:
                Driver driver = driverQueryPort.findByMember_Id(member.getId());

                // Feign으로 진행 중인 배송 확인
                BaseResponse<Boolean> response = shipmentClient.hasActiveShipments(driver.getId());
                if (response.getData() != null && response.getData()) {
                    throw new AuthException(CANNOT_DELETE_DRIVER_WITH_ACTIVE_SHIPMENT);
                }

                driverCommandPort.delete(driver.getId());
                break;

            case ADMIN:
                // 마지막 관리자인지 확인
                long adminCount = memberQueryPort.findAll().stream()
                        .filter(m -> m.getRole() == MemberRole.ADMIN)
                        .count();

                if (adminCount <= 1) {
                    throw new AuthException(CANNOT_DELETE_LAST_ADMIN);
                }
                break;

            case MANAGER:
                break;
        }

        // 사용자 정보 최종 삭제
        memberCommandPort.delete(member.getId());
    }

    @Override
    @Transactional
    public void deleteStore(Integer id) {
        // Read DB에서 Store 조회 시도 (연관 데이터 삭제용)
        Store store = null;
        try {
            store = storeQueryPort.findById(id);
        } catch (AuthException e) {
            // Read DB에 없으면 Write DB에서 바로 삭제
            storeCommandPort.delete(id);
            return;
        }

        // 연관된 POS와 Branch Manager 정보 먼저 수집
        List<Pos> posDevices = posQueryPort.findByStoreIdIn(List.of(store.getId()));
        Member branchManager = store.getMember();

        // 1. POS 삭제
        for (Pos pos : posDevices) {
            posCommandPort.delete(pos.getId());
        }

        // 2. Store 삭제 (외래키 참조 해제)
        storeCommandPort.delete(store.getId());

        // 3. POS Member 삭제
        for (Pos pos : posDevices) {
            Member posMember = pos.getMember();
            if (posMember != null) {
                memberCommandPort.delete(posMember.getId());
            }
        }

        // 4. Branch Manager 삭제
        if (branchManager != null) {
            memberCommandPort.delete(branchManager.getId());
        }
    }

    @Override
    @Transactional
    public void deletePos(Integer id) {
        Pos pos = null;
        try {
            pos = posQueryPort.findById(id);
        } catch (AuthException e) {
            // Read DB에 없으면 Write DB에서 바로 삭제
            posCommandPort.delete(id);
            return;
        }

        // Member 정보 먼저 수집
        Member posMember = pos.getMember();

        // 1. POS 삭제 (외래키 참조 해제)
        posCommandPort.delete(pos.getId());

        // 2. POS Member 삭제
        if (posMember != null) {
            memberCommandPort.delete(posMember.getId());
        }
    }

    @Override
    @Transactional
    public void deleteDriver(Integer id) {
        Driver driver = null;
        try {
            driver = driverQueryPort.findById(id);
        } catch (AuthException e) {
            // Read DB에 없으면 Write DB에서 바로 삭제
            driverCommandPort.delete(id);
            return;
        }

        // Member 정보 먼저 수집
        Member driverMember = driver.getMember();

        // Feign으로 진행 중인 배송 확인
        BaseResponse<Boolean> response = shipmentClient.hasActiveShipments(driver.getId());
        if (response.getData() != null && response.getData()) {
            throw new AuthException(CANNOT_DELETE_DRIVER_WITH_ACTIVE_SHIPMENT);
        }

        // 1. Driver 삭제 (외래키 참조 해제)
        driverCommandPort.delete(driver.getId());

        // 2. Driver Member 삭제
        if (driverMember != null) {
            memberCommandPort.delete(driverMember.getId());
        }
    }
}
