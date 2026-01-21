package se.fk.github.manuellregelratttillforsakring.logic.config;

import java.util.Date;
import java.util.UUID;

public class Lagrum
{

   private UUID id;
   private String version;
   private Date giltigFom;
   private String forfattning;
   private String kapitel;
   private String paragraf;
   private String stycke;
   private String punkt;

   public Lagrum()
   {

   }

   public UUID getId()
   {
      return id;
   }

   public void setId(UUID id)
   {
      this.id = id;
   }

   public String getVersion()
   {
      return version;
   }

   public void setVersion(String version)
   {
      this.version = version;
   }

   public Date getGiltigFom()
   {
      return giltigFom;
   }

   public void setGiltigFom(Date giltigFom)
   {
      this.giltigFom = giltigFom;
   }

   public String getForfattning()
   {
      return forfattning;
   }

   public void setForfattning(String forfattning)
   {
      this.forfattning = forfattning;
   }

   public String getKapitel()
   {
      return kapitel;
   }

   public void setKapitel(String kapitel)
   {
      this.kapitel = kapitel;
   }

   public String getParagraf()
   {
      return paragraf;
   }

   public void setParagraf(String paragraf)
   {
      this.paragraf = paragraf;
   }

   public String getStycke()
   {
      return stycke;
   }

   public void setStycke(String stycke)
   {
      this.stycke = stycke;
   }

   public String getPunkt()
   {
      return punkt;
   }

   public void setPunkt(String punkt)
   {
      this.punkt = punkt;
   }

}
