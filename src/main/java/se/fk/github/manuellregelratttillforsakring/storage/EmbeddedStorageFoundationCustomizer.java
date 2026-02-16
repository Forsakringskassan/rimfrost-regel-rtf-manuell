package se.fk.github.manuellregelratttillforsakring.storage;

import org.eclipse.store.storage.embedded.types.EmbeddedStorageFoundation;

public interface EmbeddedStorageFoundationCustomizer
{
   public <T extends EmbeddedStorageFoundation<?>> void customize(T foundation);
}
