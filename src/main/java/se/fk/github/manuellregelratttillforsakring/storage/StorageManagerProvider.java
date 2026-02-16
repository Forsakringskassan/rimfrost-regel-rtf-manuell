package se.fk.github.manuellregelratttillforsakring.storage;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.serializer.reflect.ClassLoaderProvider;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageFoundation;
import org.eclipse.store.storage.types.StorageManager;

import java.nio.file.Path;

@SuppressWarnings("unused")
@ApplicationScoped
public class StorageManagerProvider
{
   private StorageManager storageManager;

   @ConfigProperty(name = "application.storage.storage-directory", defaultValue = "data")
   String storageDirectory;

   @Inject
   Instance<EmbeddedStorageFoundationCustomizer> customizers;

   @Inject
   Instance<DataStorageProvider<?>> dataStorageProviders;

   @PostConstruct
   public void init()
   {
      EmbeddedStorageFoundation<?> foundation = EmbeddedStorage.Foundation(Path.of(storageDirectory));

      for (EmbeddedStorageFoundationCustomizer customizer : customizers)
      {
         customizer.customize(foundation);
      }

      // Workaround for quarkus modifying types at runtime - https://stackoverflow.com/questions/65898882/quarkus-with-microstream-classloader-problems.
      // Any better way of handling this?
      foundation.onConnectionFoundation(connectionFoundation -> connectionFoundation
            .setClassLoaderProvider(ClassLoaderProvider.New(Thread.currentThread().getContextClassLoader())));

      var dataStorage = dataStorageProviders.get().getDataStorage();
      storageManager = foundation.createEmbeddedStorageManager(dataStorage);
      storageManager.start();
   }

   @PreDestroy
   public void shutdown()
   {
      storageManager.shutdown();
   }

   public StorageManager getStorageManager()
   {
      return storageManager;
   }
}
