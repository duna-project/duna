package duna.eventstore.transaction

case class TransactionFailedException(message: String, cause: Throwable) extends Exception(message, cause)
