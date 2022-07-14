package net.maisikoleni.javadoc.service.jdk18;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.Jdk;
import net.maisikoleni.javadoc.service.Jdk.Version;
import net.maisikoleni.javadoc.service.SearchService;

@QuarkusTest
class Jdk18SearchSerivceTest {

	@Inject
	@Jdk(Version.RELEASE_18)
	SearchService searchService;

	@Test
	void testBaseUrl() {
		assertThat(searchService.javadoc().baseUrl())
				.isEqualTo(URI.create("https://docs.oracle.com/en/java/javase/18/docs/api/"));
	}

	@Test
	void testSearchSet() {
		var results = searchAsList("Set");
		assertThat(results.lines().limit(62).collect(Collectors.joining("\n"))).isEqualTo(expectedSearchResults("""
				java.base/java.util.Set
				java.desktop/javax.print.attribute.SetOfIntegerSyntax
				java.desktop/javax.print.attribute.SetOfIntegerSyntax.SetOfIntegerSyntax(int)
				java.desktop/javax.print.attribute.SetOfIntegerSyntax.SetOfIntegerSyntax(int, int)
				java.desktop/javax.print.attribute.SetOfIntegerSyntax.SetOfIntegerSyntax(int[][])
				java.desktop/javax.print.attribute.SetOfIntegerSyntax.SetOfIntegerSyntax(String)
				jdk.jfr/jdk.jfr.SettingControl
				jdk.jfr/jdk.jfr.SettingControl.SettingControl()
				jdk.jfr/jdk.jfr.SettingDefinition
				jdk.jfr/jdk.jfr.SettingDescriptor
				jdk.management.jfr/jdk.management.jfr.SettingDescriptorInfo
				java.base/java.util.AbstractSet
				java.desktop/javax.accessibility.AccessibleRelationSet
				java.desktop/javax.accessibility.AccessibleStateSet
				java.base/java.util.Calendar.areFieldsSet
				java.desktop/javax.print.attribute.AttributeSet
				java.desktop/javax.swing.text.AttributeSet
				java.desktop/javax.imageio.plugins.tiff.BaselineTIFFTagSet
				java.sql.rowset/javax.sql.rowset.BaseRowSet
				java.base/java.util.BitSet
				java.sql.rowset/javax.sql.rowset.CachedRowSet
				java.base/java.lang.constant.ConstantDescs.CD_Set
				java.base/java.util.concurrent.ConcurrentSkipListSet
				java.base/java.util.concurrent.CopyOnWriteArraySet
				java.desktop/javax.print.attribute.DocAttributeSet
				java.base/java.util.EnumSet
				jdk.jdi/com.sun.jdi.event.EventSet
				java.desktop/javax.imageio.plugins.tiff.ExifGPSTagSet
				java.desktop/javax.imageio.plugins.tiff.ExifInteroperabilityTagSet
				java.desktop/javax.imageio.plugins.tiff.ExifParentTIFFTagSet
				java.desktop/javax.imageio.plugins.tiff.ExifTIFFTagSet
				java.desktop/javax.imageio.plugins.tiff.FaxTIFFTagSet
				java.sql.rowset/javax.sql.rowset.FilteredRowSet
				java.desktop/javax.imageio.plugins.tiff.GeoTIFFTagSet
				java.desktop/javax.print.attribute.HashAttributeSet
				java.desktop/javax.print.attribute.HashDocAttributeSet
				java.desktop/javax.print.attribute.HashPrintJobAttributeSet
				java.desktop/javax.print.attribute.HashPrintRequestAttributeSet
				java.desktop/javax.print.attribute.HashPrintServiceAttributeSet
				java.base/java.util.HashSet
				java.base/java.util.Calendar.isSet
				java.base/java.util.Calendar.isTimeSet
				java.desktop/javax.swing.JViewport.isViewSizeSet
				java.sql.rowset/javax.sql.rowset.JdbcRowSet
				java.sql.rowset/javax.sql.rowset.JoinRowSet
				java.base/java.util.LinkedHashSet
				java.desktop/javax.swing.text.MutableAttributeSet
				java.base/java.util.NavigableSet
				java.desktop/javax.print.attribute.PrintJobAttributeSet
				java.desktop/javax.print.attribute.PrintRequestAttributeSet
				java.desktop/javax.print.attribute.PrintServiceAttributeSet
				java.sql/java.sql.ResultSet
				java.sql/javax.sql.RowSet
				java.desktop/javax.swing.text.SimpleAttributeSet
				java.base/java.util.SortedSet
				java.desktop/javax.swing.text.StyleContext.SmallAttributeSet
				java.desktop/javax.swing.text.StyleConstants.TabSet
				java.desktop/javax.swing.text.TabSet
				java.desktop/javax.imageio.plugins.tiff.TIFFTagSet
				java.desktop/javax.imageio.ImageWriteParam.tilingSet
				java.base/java.util.TreeSet
				java.sql.rowset/javax.sql.rowset.WebRowSet
				"""));
		assertThat(results).hasLineCount(798);
	}

	@Test
	void testSearchJavaModuleWithJPackageWithSTypeAndSomeAMember() {
		var results = searchAsList("java./j..S.~A");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.desktop/java.awt.Scrollbar.AccessibleAWTScrollBar
				java.base/java.util.Spliterators.AbstractDoubleSpliterator
				java.base/java.util.Spliterators.AbstractIntSpliterator
				java.base/java.util.Spliterators.AbstractLongSpliterator
				java.base/java.util.Spliterators.AbstractSpliterator
				java.desktop/java.awt.Scrollbar.addAdjustmentListener(AdjustmentListener)
				java.base/java.util.Set.addAll(Collection<? extends E>)
				java.base/java.lang.String.charAt(int)
				java.base/java.util.Set.containsAll(Collection<?>)
				java.base/java.util.Scanner.findAll(Pattern)
				java.base/java.util.Scanner.findAll(String)
				java.desktop/java.awt.Scrollbar.getAccessibleContext()
				java.desktop/java.awt.Scrollbar.getAdjustmentListeners()
				java.base/java.security.Signature.getAlgorithm()
				java.base/java.security.Security.getAlgorithmProperty(String, String)
				java.base/java.security.Security.getAlgorithms(String)
				java.desktop/java.beans.Statement.getArguments()
				java.sql/java.sql.Struct.getAttributes()
				java.sql/java.sql.Struct.getAttributes(Map<String, Class<?>>)
				java.desktop/java.awt.Scrollbar.processAdjustmentEvent(AdjustmentEvent)
				java.desktop/java.awt.Scrollbar.removeAdjustmentListener(AdjustmentListener)
				java.base/java.util.Set.removeAll(Collection<?>)
				java.base/java.lang.String.replaceAll(String, String)
				java.base/java.util.Set.retainAll(Collection<?>)
				java.base/java.util.Set.toArray()
				java.base/java.util.Set.toArray(T[])
				java.base/java.util.Spliterator.tryAdvance(Consumer<? super T>)
				"""));
	}

	@Test
	void testSearchComparableCompareTo() {
		var results = searchAsList("java.base/java.lang.Comparable.compareTo(T)");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.base/java.lang.Comparable.compareTo(T)
				"""));
	}

	@Test
	void testSearchDotCollector() {
		var results = searchAsList(".Collector");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.base/java.util.stream.Collector
				java.base/java.util.stream.Collectors
				"""));
	}

	@Test
	void testSearchLowersMathSpaceMax() {
		var results = searchAsList("math max");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.base/java.lang.Math.max(double, double)
				java.base/java.lang.Math.max(float, float)
				java.base/java.lang.Math.max(int, int)
				java.base/java.lang.Math.max(long, long)
				java.base/java.lang.StrictMath.max(double, double)
				java.base/java.lang.StrictMath.max(float, float)
				java.base/java.lang.StrictMath.max(int, int)
				java.base/java.lang.StrictMath.max(long, long)
				"""));
	}

	@Test
	void testSearchLowersAbstractSetTildes() {
		var results = searchAsList("a~s~set");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.base/java.util.AbstractSet
				java.base/java.util.AbstractSet.AbstractSet()
				"""));
	}

	@Test
	void testSearchLowersDotAttributeSetTildes() {
		var results = searchAsList(".a~set");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.desktop/javax.print.attribute.AttributeSet
				java.desktop/javax.swing.text.AttributeSet
				java.desktop/javax.print.attribute.AttributeSetUtilities
				"""));
	}

	@Test
	void testSearchIOOBE() {
		var results = searchAsList("IOOBE");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.base/java.lang.IndexOutOfBoundsException
				java.base/java.lang.IndexOutOfBoundsException.IndexOutOfBoundsException()
				java.base/java.lang.IndexOutOfBoundsException.IndexOutOfBoundsException(int)
				java.base/java.lang.IndexOutOfBoundsException.IndexOutOfBoundsException(long)
				java.base/java.lang.IndexOutOfBoundsException.IndexOutOfBoundsException(String)
				java.base/java.lang.ArrayIndexOutOfBoundsException
				java.base/java.lang.ArrayIndexOutOfBoundsException.ArrayIndexOutOfBoundsException()
				java.base/java.lang.ArrayIndexOutOfBoundsException.ArrayIndexOutOfBoundsException(int)
				java.base/java.lang.ArrayIndexOutOfBoundsException.ArrayIndexOutOfBoundsException(String)
				java.base/java.lang.StringIndexOutOfBoundsException
				java.base/java.lang.StringIndexOutOfBoundsException.StringIndexOutOfBoundsException()
				java.base/java.lang.StringIndexOutOfBoundsException.StringIndexOutOfBoundsException(int)
				java.base/java.lang.StringIndexOutOfBoundsException.StringIndexOutOfBoundsException(String)
				"""));
	}

	@Test
	void testSearchAIOOBE() {
		var results = searchAsList("aioobe^^");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.base/java.lang.ArrayIndexOutOfBoundsException
				java.base/java.lang.ArrayIndexOutOfBoundsException.ArrayIndexOutOfBoundsException()
				java.base/java.lang.ArrayIndexOutOfBoundsException.ArrayIndexOutOfBoundsException(int)
				java.base/java.lang.ArrayIndexOutOfBoundsException.ArrayIndexOutOfBoundsException(String)
				"""));
	}

	@Test
	void testSearchAwaitNoArgs() {
		var results = searchAsList("aw()");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.base/java.util.concurrent.CountDownLatch.await()
				java.base/java.util.concurrent.CyclicBarrier.await()
				java.base/java.util.concurrent.locks.AbstractQueuedLongSynchronizer.ConditionObject.await()
				java.base/java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject.await()
				java.base/java.util.concurrent.locks.Condition.await()
				"""));
	}

	@Test
	void testSearchFilesWithModule() {
		var results = searchAsList("java.base/...Files");
		assertThat(results).isEqualTo(expectedSearchResults("""
				java.base/java.nio.file.Files
				"""));
	}

	String searchAsList(String query) {
		return searchService.searchEngine().search(query).map(SearchableEntity::qualifiedName)
				.collect(Collectors.joining("\n"));
	}

	static String expectedSearchResults(String text) {
		return text.strip();
	}
}
