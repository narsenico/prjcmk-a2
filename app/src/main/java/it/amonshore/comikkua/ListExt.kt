package it.amonshore.comikkua

suspend fun <T, R> List<T>.letNotEmpty(block: suspend (List<T>) -> R): R? {
    if (isEmpty()) {
        return null
    }

    return block(this)
}