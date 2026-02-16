package se.fk.github.manuellregelratttillforsakring.storage;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestDataStorageProvider implements DataStorageProvider<TestDataStorage>
{
   private TestDataStorage testDataStorage;

   @PostConstruct
   void init()
   {
      testDataStorage = new TestDataStorage();
   }

   @Override
   public TestDataStorage getDataStorage()
   {
      return testDataStorage;
   }
}
