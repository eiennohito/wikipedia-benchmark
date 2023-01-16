package wiki.handlers;

import info.bliki.htmlcleaner.TagNode;
import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.filter.PlainTextConvertable;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.model.ImageFormat;

import java.io.IOException;
import java.util.List;

import static info.bliki.wiki.model.Configuration.RENDERER_RECURSION_LIMIT;

public class ParagraphConverter implements ITextConverter {

    private final List<String> result;
    private final StringBuilder buffer = new StringBuilder();

    public ParagraphConverter(List<String> result) {
        this.result = result;
    }


    @Override
    public void nodesToText(List<?> nodes, Appendable resultBuffer, IWikiModel model) throws IOException {
        assert (model != null);
        if (nodes != null && !nodes.isEmpty()) {
            try {
                int level = model.incrementRecursionLevel();

                if (level > RENDERER_RECURSION_LIMIT) {
                    resultBuffer.append("Error - recursion limit exceeded rendering tags in PlainTextConverter#nodesToText().");
                    return;
                }

                if (level == 1) {
                    resultBuffer = buffer;
                }

                for (Object item : nodes) {
                    if (item instanceof List<?>) {
                        nodesToText((List<?>) item, resultBuffer, model);
                    } else if (item instanceof PlainTextConvertable) {
                        ((PlainTextConvertable) item).renderPlainText(this, resultBuffer, model);
                    }
                    if (level == 1) {
                        StringBuilder buf = buffer;
                        String data = buf.toString().strip();
                        if (!data.isEmpty()) {
                            result.add(data);
                        }
                        if (buf.length() > 0) {
                            buf.delete(0, buffer.length() - 1);
                        }
                    }
                }
            } finally {
                model.decrementRecursionLevel();
            }
        }
    }

    @Override
    public boolean renderLinks() {
        return false;
    }

    @Override
    public void imageNodeToText(TagNode imageTagNode, ImageFormat imageFormat, Appendable resultBuffer, IWikiModel model)
            throws IOException {
    }
}

