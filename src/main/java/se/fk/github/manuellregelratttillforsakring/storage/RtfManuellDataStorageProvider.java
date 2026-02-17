package se.fk.github.manuellregelratttillforsakring.storage;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.storage.entity.RtfManuellDataStorage;
import se.fk.rimfrost.framework.storage.DataStorageProvider;

@ApplicationScoped
public class RtfManuellDataStorageProvider implements DataStorageProvider<RtfManuellDataStorage>
{
   private RtfManuellDataStorage storage;

   @PostConstruct
   public void init()
   {
      storage = new RtfManuellDataStorage();
   }

   @Override
   public RtfManuellDataStorage getDataStorage()
   {
      return storage;
   }
}
