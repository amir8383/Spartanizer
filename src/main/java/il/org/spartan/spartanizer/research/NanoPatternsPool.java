package il.org.spartan.spartanizer.research;

import java.util.*;

import org.eclipse.jdt.core.dom.*;

/** @author Ori Marcovitch
 * @since 2016 */
public class NanoPatternsPool {
  public static final Map<Class<?>, List<NanoPattern>> nanoPatterns = new HashMap<>();

  public static void add(final Class<?> c, final NanoPattern p) {
    if (!nanoPatterns.containsKey(c))
      nanoPatterns.put(c, new ArrayList<>());
    nanoPatterns.get(c).add(p);
  }

  public static void add(final Class<?> c, final NanoPattern... nps) {
    for (final NanoPattern ¢ : nps)
      add(c, ¢);
  }

  /** return all NanoPatterns for class @link{¢}
   * @param ¢
   * @return */
  public static Collection<NanoPattern> get(final Class<? extends ASTNode> ¢) {
    return !nanoPatterns.containsKey(¢) ? new ArrayList<>() : nanoPatterns.get(¢);
  }
}