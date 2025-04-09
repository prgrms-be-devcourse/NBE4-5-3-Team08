package com.team8.project2.domain.link.service

import com.team8.project2.domain.link.entity.Link
import com.team8.project2.domain.link.repository.LinkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class LinkClickService(
    private val linkRepository: LinkRepository
) {

    /**
     * 링크의 클릭수를 증가시킵니다.
     *
     * @param link 클릭수 증가 대상 링크
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 별도 트랜잭션 실행
    fun increaseClickCount(link: Link) {
        link.click = link.click + 1
        linkRepository.save(link)
        linkRepository.flush() // 즉시 반영
    }
}
