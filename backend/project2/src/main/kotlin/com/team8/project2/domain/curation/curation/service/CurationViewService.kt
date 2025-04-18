package com.team8.project2.domain.curation.curation.service

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class CurationViewService(private val curationRepository: CurationRepository) {
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 별도 트랜잭션 실행
    fun increaseViewCount(curation: Curation) {
        // 조회수 증가
        curation.increaseViewCount()
        // DB에 저장
        curationRepository.save(curation)
        curationRepository.flush() // 즉시 반영
    }
}

