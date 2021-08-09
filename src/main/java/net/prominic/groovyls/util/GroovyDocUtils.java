package net.prominic.groovyls.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import groovy.lang.groovydoc.Groovydoc;

import org.codehaus.groovy.ast.AnnotatedNode;

/**
 * Functions for retrieving and formatting GroovyDoc strings.
 */
public class GroovyDocUtils {

  /**
   * Gets the docString for a node and converts it to markdown. If the node
   * doesn't have a docString then null is returned.
   * 
   * @param node The AST node to check for a docString.
   * @return The converted docString or null.
   */
  public static String getDocString(AnnotatedNode node) {

    Groovydoc docstring = node.getGroovydoc();
    String content = null;

    if (docstring.isPresent()) {
      content = convertToMarkdown(docstring.getContent());
    }

    return content;
  }

  /**
   * Converts a docString into Markdown. The comment markers will be removed.
   * Parameters and the return value will be turned into a bullet list. All other
   * tags will be removed. HTML will be left untouched but will likely be stripped
   * by the LS client.
   * 
   * @param text The text you want to convert.
   * @return The text converted to Markdown.
   */
  public static String convertToMarkdown(String text) {

    String result;

    result = removeComment(text);

    result = convertParams(result);

    result = convertReturn(result);

    result = removeTags(result);

    result = convertNewLines(result);

    return result;

  }

  /**
   * Takes a commented block of text and removes the comment, leaving the content
   * as is.
   * 
   * @param text The commented block of text.
   * @return The content from inside the comment block.
   */
  public static String removeComment(String text) {

    String result;
    Pattern pattern;
    Matcher matcher;

    // Get the content from "* some content\n" to "some content\n"
    final String content = "^[\\h]*\\*\\h?(.*\\n)";
    final String cSubst = "$1";

    pattern = Pattern.compile(content, Pattern.MULTILINE);
    matcher = pattern.matcher(text);

    result = matcher.replaceAll(cSubst);

    // Strips the leading comment "/**""
    final String stripLeading = "^\\h?\\/\\*{2}\\s*";
    final String lSubst = "";

    pattern = Pattern.compile(stripLeading, Pattern.MULTILINE);
    matcher = pattern.matcher(result);

    result = matcher.replaceAll(lSubst);

    // Strips the trailing comment "\*"
    final String stripTrailing = "\\h?\\*\\/\\s?$";
    final String tSubst = "";

    pattern = Pattern.compile(stripTrailing, Pattern.MULTILINE);
    matcher = pattern.matcher(result);

    result = matcher.replaceAll(tSubst);

    return result;

  }

  /**
   * Converts GroovyDoc param tags to Markdown bullet list. If no tags are found
   * then the original text is returned.
   * 
   * @param text The docString to convert.
   * @return The text with param tags converted to Markdown bullet list.
   */
  protected static String convertParams(String text) {
    final String regex = "^\\h*\\@(param)\\h+(\\w*)\\h+(.*)$";

    final String subst = "- *$1* **$2** - $3";

    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    final Matcher matcher = pattern.matcher(text);

    final String result = matcher.replaceAll(subst);

    return result;
  }

  /**
   * Converts GroovyDoc return tag to Markdown bullet list. If no return tag is
   * found then the original text is returned.
   * 
   * @param text The docString to convert.
   * @return The text with return tag converted to Markdown bullet list.
   */
  protected static String convertReturn(String text) {
    final String regex = "^\\h*\\@(return)\\h+(.*)$";

    final String subst = "- *$1* - $2";

    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    final Matcher matcher = pattern.matcher(text);

    final String result = matcher.replaceAll(subst);

    return result;

  }

  /**
   * Removes all GroovyDoc tags from a string. If no tags are found then the
   * original text is returned.
   * 
   * @param text The docString to convert.
   * @return The text with all GroovyDoc tags removed.
   */
  protected static String removeTags(String text) {
    final String regex = "^\\h?\\@.*\\n?";

    final String subst = "";

    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    final Matcher matcher = pattern.matcher(text);

    // The substituted value will be contained in the result variable
    final String result = matcher.replaceAll(subst);

    return result;
  }

  /**
   * Converts \n lines into Markdown newlines by prepending two spaces like " \n"
   * 
   * @param text The docString to convert.
   * @return The text with Markdown newlines.
   */
  protected static String convertNewLines(String text) {

    final String regex = "\\n";

    final String subst = "  \n";

    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    final Matcher matcher = pattern.matcher(text);

    final String result = matcher.replaceAll(subst);

    return result;

  }

}
