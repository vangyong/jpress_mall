package io.jpress.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class CollectionUtils {
	
	 public static boolean isEmpty(Object object)
	  {
	    if (String.class.isInstance(object)) {
	      String string = (String)object;
	      return string == null;
	    }

	    if (Collection.class.isInstance(object)) {
	      Collection col = (Collection)object;
	      return col == null ? true : col.isEmpty();
	    }

	    if (Map.class.isInstance(object)) {
	      Map map = (Map)object;
	      return map == null ? true : map.isEmpty();
	    }

	    if (Vector.class.isInstance(object)) {
	      Vector vector = (Vector)object;
	      return vector == null;
	    }

	    if (java.lang.Object.class.isInstance(object)) {
	      Object[] obj = (Object[])object;
	      return obj == null;
	    }

	    return object == null;
	  }

	  public static boolean isNotEmpty(Object object)
	  {
	    return !isEmpty(object);
	  }

	  public static List removeDuplication(List list)
	  {
	    Set set = new HashSet();
	    set.addAll(list);
	    list.clear();
	    return new ArrayList(set);
	  }

	  public static Map sortByValue(Map map)
	  {
	    List list = new LinkedList(map.entrySet());
	    Collections.sort(list, new Comparator() {
	    	
	      public int compare(Object o1, Object o2) {
	        return ((Comparable)((Map.Entry)o1).getValue()).compareTo(((Map.Entry)o2).getValue());
	      }
	      
	    });
	    Map result = new LinkedHashMap();
	    for (Iterator it = list.iterator(); it.hasNext(); ) {
	      Map.Entry entry = (Map.Entry)it.next();
	      result.put(entry.getKey(), entry.getValue());
	    }
	    return result;
	  }

	  public static Map sortByValue(Map map, boolean reverse)
	  {
	    List list = new LinkedList(map.entrySet());
//	    Collections.sort(list, new Comparator(reverse) {
//	      public int compare(Object o1, Object o2) {
//	        if (this.val$reverse) {
//	          return -((Comparable)((Map.Entry)o1).getValue()).compareTo(((Map.Entry)o2).getValue());
//	        }
//	        return ((Comparable)((Map.Entry)o1).getValue()).compareTo(((Map.Entry)o2).getValue());
//	      }
//	    });
	    Map result = new LinkedHashMap();
	    for (Iterator it = list.iterator(); it.hasNext(); ) {
	      Map.Entry entry = (Map.Entry)it.next();
	      result.put(entry.getKey(), entry.getValue());
	    }
	    return result;
	  }

	  public static <K, V> Map<K, V> sortByValue(Map<K, V> map, Comparator<K> comparator)
	  {
	    List list = new LinkedList(map.keySet());
	    Collections.sort(list, comparator);
	    Map result = new LinkedHashMap();
	    int i = 0; for (int len = list.size(); i < len; i++) {
	      Object key = list.get(i);
	      Object value = map.get(key);
	      result.put(key, value);
	    }
	    return result;
	  }

}
