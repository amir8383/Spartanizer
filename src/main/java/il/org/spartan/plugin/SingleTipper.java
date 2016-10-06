package il.org.spartan.plugin;

import static il.org.spartan.plugin.RefactorerUtil.*;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.text.*;

import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.tipping.*;

/** Single tipper applicator implementation using modified {@link Trimmer}
 * @author Ori Roth
 * @since 2016 */
public class SingleTipper<N extends ASTNode> extends Trimmer {
  final Tipper<N> tipper;

  public SingleTipper(final Tipper<N> tipper) {
    this.tipper = tipper;
  }

  @Override protected boolean check(final ASTNode ¢) {
    return Toolbox.defaultInstance().get(¢.getNodeType()).contains(tipper);
  }

  @SuppressWarnings("unchecked") @Override protected Tipper<N> getTipper(final ASTNode ¢) {
    assert check(¢);
    return !tipper.canTip((N) ¢) ? null : tipper;
  }

  /** Marker resolution. Commits single tipper in current function.
   * @author Ori Roth
   * @since 2016 */
  static class InDeclaration extends Resolution {
    @Override protected ITextSelection domain(final IMarker m) {
      final ICompilationUnit u = eclipse.currentCompilationUnit();
      if (u == null)
        return null;
      final ASTNode n = eclipse.getNodeByMarker(u, m);
      if (n == null)
        return null;
      final ASTNode d = searchAncestors.forClass(BodyDeclaration.class).from(n);
      return d == null ? null : new TextSelection(d.getStartPosition(), d.getLength());
    }

    @Override public String getLabelSuffix() {
      return "enclosing function";
    }

    @Override public List<ICompilationUnit> getTargetCompilationUnits() {
      return Collections.singletonList(eclipse.currentCompilationUnit());
    }
  }

  /** Marker resolution. Commits single tipper in current file.
   * @author Ori Roth
   * @since 2016 */
  static class InFile extends Resolution {
    @Override protected ITextSelection domain(@SuppressWarnings("unused") final IMarker __) {
      return TextSelection.emptySelection();
    }

    @Override public String getLabelSuffix() {
      return "compilation unit";
    }

    @Override public List<ICompilationUnit> getTargetCompilationUnits() {
      return Collections.singletonList(eclipse.currentCompilationUnit());
    }
  }

  /** Marker resolution. Commits single tipper in current project.
   * @author Ori Roth
   * @since 2016 */
  static class InProject extends Resolution {
    @Override protected ITextSelection domain(@SuppressWarnings("unused") final IMarker __) {
      return TextSelection.emptySelection();
    }

    @Override public String getLabelSuffix() {
      return "entire project";
    }

    @Override public List<ICompilationUnit> getTargetCompilationUnits() {
      try {
        return eclipse.compilationUnits(eclipse.currentCompilationUnit(), new NullProgressMonitor());
      } catch (final JavaModelException x) {
        monitor.log(x);
        return Collections.emptyList();
      }
    }

    @Override public String getOpeningMessage(final Map<attribute, Object> ¢) {
      return "Applying " + getTipperName(¢) + " to " + projectName(¢) + " with " + getCUsCount(¢) + " files";
    }

    @SuppressWarnings("boxing") @Override public String getEndingMessage(final Map<attribute, Object> ¢) {
      return "Done applying " + getTipperName(¢) + " to " + projectName(¢) + ", " + getChangesCount(¢) + " files spartanized in "
          + ¢.get(attribute.PASSES) + " " + plurales("pass", (int) ¢.get(attribute.PASSES));
    }

    @Override public String getProgressMonitorSubMessage(final List<ICompilationUnit> currentCompilationUnits,
        final ICompilationUnit currentCompilationUnit) {
      return completionIndex(currentCompilationUnits, currentCompilationUnit) + " : " + currentCompilationUnit.getElementName();
    }

    @Override public int getProgressMonitorWork(final List<ICompilationUnit> ¢) {
      return ¢.size();
    }
  }

  abstract static class Resolution extends Refactorer {
    @Override public boolean isMarkerResolution() {
      return true;
    }

    @Override public String getLabel() {
      return "Apply to " + getLabelSuffix();
    }

    protected abstract ITextSelection domain(IMarker m);

    public abstract String getLabelSuffix();

    @SuppressWarnings({ "unchecked", "rawtypes" }) @Override public GUI$Applicator getApplicator(final IMarker m) {
      try {
        assert m.getAttribute(Builder.SPARTANIZATION_TIPPER_KEY) != null;
        if (m.getResource() == null)
          return null;
        final GUI$Applicator $ = getSingleTipper((Class<? extends Tipper>) m.getAttribute(Builder.SPARTANIZATION_TIPPER_KEY));
        if ($ == null)
          return null;
        final ITextSelection t = domain(m);
        if (t == null)
          return null;
        $.setSelection(t);
        return $;
      } catch (final CoreException x) {
        monitor.log(x);
      }
      return null;
    }

    @Override public int passesCount() {
      return MANY_PASSES;
    }

    @SuppressWarnings("unused") private static <X extends ASTNode, T extends Tipper<X>> SingleTipper<X> getSingleTipper(final Class<T> t) {
      try {
        return new SingleTipper<>(t.newInstance());
      } catch (InstantiationException | IllegalAccessException x) {
        monitor.log(x);
      }
      return null;
    }
  }
}