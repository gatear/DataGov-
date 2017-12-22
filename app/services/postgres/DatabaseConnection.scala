package services.postgres

import javax.inject.{Inject, Singleton}

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.postgresql.util.URLParser
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class DatabaseConnection {

  val configuration = URLParser.parse("jdbc:postgres://localhost:5432/postgres?user=postgres&password=root")

  private val factory = new PostgreSQLConnectionFactory( configuration )

  private val pool = new ConnectionPool(factory, PoolConfiguration.Default)

  val messagesRepository = new MessageRepository( pool )
}
