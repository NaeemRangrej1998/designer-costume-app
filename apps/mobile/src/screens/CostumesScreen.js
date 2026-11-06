import { useCallback, useEffect, useRef, useState } from "react";
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, Text, View } from "react-native";
import { Screen, Heading } from "../components/Screen";
import { Button, Field } from "../components/UI";
import { CostumeCard } from "../components/CatalogCards";
import { getCategories, getCostumes } from "../services/catalogService";
import { getApiErrorMessage } from "../services/apiClient";
import { theme } from "../constants/theme";

const PAGE_SIZE = 12;

export default function CostumesScreen({ navigation, route }) {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [categoriesError, setCategoriesError] = useState("");
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [categoryId, setCategoryId] = useState(route.params?.categoryId || "");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const requestId = useRef(0);

  useEffect(() => {
    if (route.params?.categoryId) setCategoryId(route.params.categoryId);
  }, [route.params?.categoryId]);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search.trim()), 350);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    getCategories()
      .then((result) => { setCategories(result.content || []); setCategoriesError(""); })
      .catch((requestError) => setCategoriesError(getApiErrorMessage(requestError, "Category filters could not be loaded.")));
  }, []);

  const loadPage = useCallback(async (nextPage, replace = false) => {
    const currentRequest = ++requestId.current;
    if (replace) { setLoading(true); setError(""); }
    else setLoadingMore(true);
    try {
      const result = await getCostumes({ page: nextPage, size: PAGE_SIZE, search: debouncedSearch, categoryId });
      if (currentRequest !== requestId.current) return;
      setItems((current) => replace ? (result.content || []) : [...current, ...(result.content || [])]);
      setPage(result.page || nextPage);
      setTotalPages(result.totalPages || 0);
    } catch (requestError) {
      if (currentRequest === requestId.current) setError(getApiErrorMessage(requestError, "Costumes could not be loaded."));
    } finally {
      if (currentRequest === requestId.current) {
        setLoading(false);
        setLoadingMore(false);
        setRefreshing(false);
      }
    }
  }, [debouncedSearch, categoryId]);

  useEffect(() => { setItems([]); loadPage(0, true); }, [loadPage]);

  const loadMore = () => {
    if (!loading && !loadingMore && page + 1 < totalPages) loadPage(page + 1);
  };

  const renderHeader = (
    <View style={styles.header}>
      <Heading eyebrow="THE BOUTIQUE COLLECTION" title={route.params?.categoryName || "Designer costumes"} description="Search the live catalog and filter by occasion." />
      <Field label="Search costumes" value={search} onChangeText={setSearch} placeholder="Search the collection" returnKeyType="search" />
      <Text style={styles.filterTitle}>Occasion</Text>
      {categoriesError ? <Text style={styles.error}>{categoriesError}</Text> : null}
      <View style={styles.filters}>
        <Button title="All" variant={categoryId ? "quiet" : "secondary"} onPress={() => { setCategoryId(""); setPage(0); }} style={styles.filterButton} />
        {categories.map((category) => (
          <Button key={category.id} title={category.name} variant={categoryId === category.id ? "secondary" : "quiet"} onPress={() => { setCategoryId(category.id); setPage(0); }} style={styles.filterButton} />
        ))}
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );

  return (
    <Screen scroll={false}>
      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        numColumns={2}
        columnWrapperStyle={styles.row}
        contentContainerStyle={styles.list}
        ListHeaderComponent={renderHeader}
        ListEmptyComponent={loading ? <ActivityIndicator color={theme.colors.accent} /> : error ? <Button title="Retry" variant="secondary" onPress={() => loadPage(0, true)} /> : <Text style={styles.empty}>No costumes match these filters.</Text>}
        ListFooterComponent={loadingMore ? <ActivityIndicator color={theme.colors.accent} /> : page + 1 < totalPages ? <Button title="Load more" variant="secondary" onPress={loadMore} /> : null}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); loadPage(0, true); }} tintColor={theme.colors.accent} />}
        onEndReached={loadMore}
        onEndReachedThreshold={0.4}
        renderItem={({ item }) => <CostumeCard costume={item} onPress={() => navigation.navigate("CostumeDetails", { costumeId: item.id })} />}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  list: { padding: 20, paddingBottom: 38, gap: 14 },
  header: { gap: 10, marginBottom: 6 },
  row: { gap: 12 },
  filters: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  filterButton: { minHeight: 36, paddingHorizontal: 12 },
  filterTitle: { fontSize: 13, color: theme.colors.ink, fontWeight: "700" },
  error: { color: theme.colors.error, fontSize: 13 },
  empty: { color: theme.colors.muted, paddingVertical: 24, textAlign: "center" },
});
