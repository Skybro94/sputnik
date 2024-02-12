package pl.touk.sputnik.engine.visitor;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import pl.touk.sputnik.review.Review;
import pl.touk.sputnik.review.ReviewFile;

import java.util.regex.Pattern;

@Slf4j
public class GlobFilterFilesVisitor implements BeforeReviewVisitor {

    private final Pattern pattern;

    // From https://stackoverflow.com/a/1248627/6222104
    private static String convertGlobToRegEx(String line) {
        line = line.trim();
        int strLen = line.length();
        StringBuilder sb = new StringBuilder(strLen);
        // Remove beginning and ending * globs because they're useless
        if (line.startsWith("*"))
        {
            line = line.substring(1);
            strLen--;
        }
        if (line.endsWith("*"))
        {
            line = line.substring(0, strLen-1);
            strLen--;
        }
        boolean escaping = false;
        int inCurlies = 0;
        for (char currentChar : line.toCharArray())
        {
            switch (currentChar)
            {
            case '*':
                if (escaping)
                    sb.append("\\*");
                else
                    sb.append(".*");
                escaping = false;
                break;
            case '?':
                if (escaping)
                    sb.append("\\?");
                else
                    sb.append('.');
                escaping = false;
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
                sb.append('\\');
                sb.append(currentChar);
                escaping = false;
                break;
            case '\\':
                if (escaping)
                {
                    sb.append("\\\\");
                    escaping = false;
                }
                else
                    escaping = true;
                break;
            case '{':
                if (escaping)
                {
                    sb.append("\\{");
                }
                else
                {
                    sb.append('(');
                    inCurlies++;
                }
                escaping = false;
                break;
            case '}':
                if (inCurlies > 0 && !escaping)
                {
                    sb.append(')');
                    inCurlies--;
                }
                else if (escaping)
                    sb.append("\\}");
                else
                    sb.append("}");
                escaping = false;
                break;
            case ',':
                if (inCurlies > 0 && !escaping)
                {
                    sb.append('|');
                }
                else if (escaping)
                    sb.append("\\,");
                else
                    sb.append(",");
                break;
            default:
                escaping = false;
                sb.append(currentChar);
            }
        }
        return sb.toString();
    }

    public GlobFilterFilesVisitor(String regex) {
        this.pattern = Pattern.compile(convertGlobToRegEx(regex));
    }

    @Override
    public void beforeReview(@NotNull Review review) {
        log.info("Filtering out test files from review using glob matching");
        review.setFiles(FluentIterable.from(review.getFiles()).filter(new Predicate<ReviewFile>() {
            @Override
            public boolean apply(ReviewFile file) {
                return pattern.matcher(file.getReviewFilename()).matches();
            }
        }).toList());
    }
}
