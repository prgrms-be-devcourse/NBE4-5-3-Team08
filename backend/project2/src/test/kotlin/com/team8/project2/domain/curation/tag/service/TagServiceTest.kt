package com.team8.project2.domain.curation.tag.service

import com.team8.project2.domain.curation.tag.entity.Tag
import com.team8.project2.domain.curation.tag.repository.TagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class TagServiceTest {
    @Mock
    lateinit var tagRepository: TagRepository

    @InjectMocks
    lateinit var tagService: TagService

    private lateinit var tag: Tag

    @BeforeEach
    fun setUp() {
        tag = Tag("testTag")
    }

    @Test
    @DisplayName("태그가 존재하면 존재하는 태그를 반환한다.")
    fun getTag_WhenTagExists_ShouldReturnExistingTag() {
        // given
        `when`(tagRepository.findByName("testTag")).thenReturn(tag)

        // when
        val result = tagService.getTag("testTag")

        // then
        assertThat(result).isEqualTo(tag)
        verify(tagRepository, times(1)).findByName("testTag")
        verify(tagRepository, never()).save(any(Tag::class.java))
    }

    @Test
    @DisplayName("태그가 존재하지 않으면 태그를 생성하고 반환한다.")
    fun getTag_WhenTagDoesNotExist_ShouldCreateAndReturnNewTag() {
        // given
        `when`(tagRepository.findByName("newTag")).thenReturn(null)
        `when`(tagRepository.save(any(Tag::class.java))).thenAnswer { it.arguments[0] }

        // when
        val result = tagService.getTag("newTag")

        // then
        assertThat(result.name).isEqualTo("newTag")
        verify(tagRepository).findByName("newTag")
        verify(tagRepository).save(any(Tag::class.java))
    }
}
