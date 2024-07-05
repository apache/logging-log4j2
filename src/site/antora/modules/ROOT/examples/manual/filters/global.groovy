Throwable lastParam = parameters?.last() instanceof Throwable ? parameters.last() : null
Throwable actualThrowable = throwable ?: message?.throwable ?: lastParam
return actualThrowable instanceof DataAccessException