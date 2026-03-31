package se.fk.github.manuellregelratttillforsakring.storage;

import java.util.HashMap;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;

@ApplicationScoped
public class ManuellRegelCommonDataStorageService implements ManuellRegelCommonDataStorage
{

   HashMap<UUID, ManuellRegelCommonData> map = new HashMap<>();

   @Override
   public ManuellRegelCommonData getManuellRegelCommonData(UUID handlaggningId)
   {
      return map.get(handlaggningId);
   }

   @Override
   public void setManuellRegelCommonData(UUID handlaggningId, ManuellRegelCommonData manuellRegelCommonData)
   {
      map.put(handlaggningId, manuellRegelCommonData);
   }

   @Override
   public void deleteManuellRegelCommonData(UUID handlaggningId)
   {
      map.remove(handlaggningId);
   }

}
