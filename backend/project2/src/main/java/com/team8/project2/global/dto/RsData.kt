package com.team8.project2.global.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RsData<T>(
    val code: String,
    val msg: String,
    val data: T? = null
) {
    @get:JsonIgnore
    val statusCode: Int
        get() = code.split("-")[0].toInt()

    // Java 에서 default 파라미터 지원이 안되어서 임시로 사용 -> Kotlin 전환 완료 시 제거
    constructor(code: String, msg: String) : this(code, msg, null)

    companion object {
        @JvmStatic
        fun <T> success(msg: String = "Success", data: T): RsData<T> {
            return RsData("200-1", msg, data)
        }

        @JvmStatic
        fun success(msg: String = "Success"): RsData<Unit> {
            return RsData("200-1", msg, Unit)
        }

        @JvmStatic
        fun <T> fail(code: String = "400-1", msg: String = "요청이 올바르지 않습니다."): RsData<T> {
            return RsData(code, msg, null)
        }

    }
}
