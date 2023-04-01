package it.amonshore.comikkua

sealed class ResultEx<out T, out E> {

    abstract val isSuccess: Boolean

    inline fun <V> map(transform: (T) -> V): ResultEx<V, E> {
        return flatMap { Success(transform(it)) }
    }

    data class Success<out T>(val value: T) : ResultEx<T, Nothing>() {
        override val isSuccess: Boolean = true
    }

    data class Failure<out E>(val error: E) : ResultEx<Nothing, E>() {
        override val isSuccess: Boolean = false
    }

    companion object {
        fun Success() = Success(Unit)
    }
}

inline fun <T, E, V> ResultEx<T, E>.flatMap(transform: (value: T) -> ResultEx<V, E>): ResultEx<V, E> {
    return when (this) {
        is ResultEx.Success -> transform(this.value)
        is ResultEx.Failure -> this
    }
}

inline fun <T, E> ResultEx<T, E>.recoverWith(recover: (E) -> ResultEx.Success<T>): ResultEx<T, E> {
    return when (this) {
        is ResultEx.Success -> this
        is ResultEx.Failure -> recover(this.error)
    }
}

inline fun <T, E> ResultEx<T, E>.recover(recover: (E) -> T): ResultEx<T, E> {
    return recoverWith { ResultEx.Success(recover(it)) }
}

inline fun <T, E> ResultEx<T, E>.getOrElse(block: () -> T): T {
    return when (this) {
        is ResultEx.Success -> this.value
        is ResultEx.Failure -> block()
    }
}

fun <T, E> ResultEx<T, E>.getOrNull(): T? {
    return when (this) {
        is ResultEx.Success -> this.value
        is ResultEx.Failure -> null
    }
}

fun <T, E> ResultEx<T, E>.getOrThrow(): T {
    return when (this) {
        is ResultEx.Success -> this.value
        is ResultEx.Failure -> throw RuntimeException("Excepted Success, found Failure")
    }
}

inline fun <T, E> ResultEx<T, E>.onSuccess(block: (value: T) -> Unit): ResultEx<T, E> {
    if (this is ResultEx.Success) {
        block(this.value)
    }
    return this
}

inline fun <T, E> ResultEx<T, E>.onFailure(block: (error: E) -> Unit): ResultEx<T, E> {
    if (this is ResultEx.Failure) {
        block(this.error)
    }
    return this
}

fun <T, E> T.toSuccess(): ResultEx<T, E> {
    return ResultEx.Success(this)
}

fun <T, E> E.toFailure(): ResultEx<T, E> {
    return ResultEx.Failure(this)
}

inline fun <E> Boolean.orFailWith(block: () -> ResultEx<Unit, E>): ResultEx<Unit, E> {
    if (!this) {
        return block()
    }

    return ResultEx.Success()
}

inline fun <E> Boolean.orFail(block: () -> E): ResultEx<Unit, E> {
    return orFailWith { block().toFailure() }
}