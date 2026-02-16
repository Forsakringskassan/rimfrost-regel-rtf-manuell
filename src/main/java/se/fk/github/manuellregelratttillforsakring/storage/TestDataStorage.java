package se.fk.github.manuellregelratttillforsakring.storage;

import java.util.ArrayList;

public class TestDataStorage
{
   private String text = "foo";

   private int count = 0;

   private ArrayList<Object> list = new ArrayList<>();

   public String getText()
   {
      return text;
   }

   public void setText(String text)
   {
      this.text = text;
   }

   public int getCount()
   {
      return count;
   }

   public void setCount(int count)
   {
      this.count = count;
   }

   public ArrayList<Object> getList()
   {
      return list;
   }

   public void setList(ArrayList<Object> list)
   {
      this.list = list;
   }
}
