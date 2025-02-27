package com.github.jarva.arsadditions.client.util;

import com.github.jarva.arsadditions.mixin.BookEntryAccessor;
import com.github.jarva.arsadditions.mixin.BookPageAccessor;
import com.github.jarva.arsadditions.mixin.PageRelationsAccessor;
import com.github.jarva.arsadditions.mixin.PageTextAccessor;
import com.hollingsworth.arsnouveau.ArsNouveau;
import net.minecraft.resources.ResourceLocation;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageRelations;
import vazkii.patchouli.client.book.page.PageText;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class BookUtil {
    public static final ResourceLocation WORN_NOTEBOOK = ArsNouveau.prefix("worn_notebook");

    public static void addRelation(ResourceLocation bookEntry, ResourceLocation relation) {
        Map<ResourceLocation, BookEntry> entries = getEntries();
        BookEntry entry = entries.get(bookEntry);
        if (entry == null) return;

        List<BookPage> pages = entry.getPages();
        Optional<BookPage> relationsPage = pages.stream().filter(p -> p instanceof PageRelations).findFirst();
        relationsPage.ifPresent(relations -> {
            PageRelationsAccessor relationsAccessor = (PageRelationsAccessor) relations;
            relationsAccessor.getEntries().add(entries.get(relation));
        });
    }

    public static void addPage(ResourceLocation bookEntry, BookPage newPage) {
        addPage(bookEntry, newPage, true, null);
    }

    public static void addPage(ResourceLocation bookEntry, BookPage newPage, boolean after, @Nullable Predicate<BookPage> isPage) {
        Map<ResourceLocation, BookEntry> entries = getEntries();
        BookEntry entry = entries.get(bookEntry);
        if (entry == null) return;

        BookEntryAccessor entryAccessor = (BookEntryAccessor) (Object) entry;
        List<BookPage> pages = entryAccessor.getRealPages();

        int i = 0;

        if (isPage == null) {
            i = pages.size() - 1;
        } else {
            for (; i < pages.size(); i++) {
                BookPage page = pages.get(i);
                if (isPage.test(page)) {
                    if (after) i++;
                    break;
                }
            }
        }

        pages.add(i, newPage);
    }

    public static BookPage newTextPage(String title, String text) {
        PageText page = new PageText();
        PageTextAccessor pageTextAccessor = (PageTextAccessor) page;
        pageTextAccessor.setTitle(title);
        BookPageAccessor bookPageAccessor = (BookPageAccessor) page;
        bookPageAccessor.setPageNum(1);
        page.setText(text);
        return page;
    }

    private static Map<ResourceLocation, BookEntry> getEntries() {
        Book wornNotebook = BookRegistry.INSTANCE.books.get(WORN_NOTEBOOK);

        return wornNotebook.getContents().entries;
    }
}
