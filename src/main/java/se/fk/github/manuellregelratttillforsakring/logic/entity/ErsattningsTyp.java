package se.fk.github.manuellregelratttillforsakring.logic.entity;

public class ErsattningsTyp
{
   private final String id;
   private final String namn;

   public ErsattningsTyp(String id, String namn)
   {
      this.id = id;
      this.namn = namn;
   }

   public String getId()
   {
      return id;
   }

   public String getNamn()
   {
      return namn;
   }

   @Override
   public String toString()
   {
      return "ErsattningsTyp{" +
            "id='" + id + '\'' +
            ", namn='" + namn + '\'' +
            '}';
   }
}
