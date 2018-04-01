import com.google.inject.AbstractModule
import java.time.Clock

import bayes.Classifier
import play.api.Logger

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule {

  override def configure() = {

    Logger.info("Binding bayes classifier ...")
    bind(classOf[Classifier]).asEagerSingleton()

    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)

    import services.postgres.DatabaseConnection
    bind(classOf[DatabaseConnection]).toInstance(new DatabaseConnection())

  }

}
