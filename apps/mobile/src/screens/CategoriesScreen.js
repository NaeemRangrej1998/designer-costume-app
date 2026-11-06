import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, Text, View } from "react-native";
import { Screen, Heading } from "../components/Screen";
import { Button } from "../components/UI";
import { CategoryCard } from "../components/CatalogCards";
import { getCategories } from "../services/catalogService";
import { getApiErrorMessage } from "../services/apiClient";
import { theme } from "../constants/theme";

export default function CategoriesScreen({ navigation }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setError("");
    try { setItems((await getCategories()).content || []); }
    catch (requestError) { setError(getApiErrorMessage(requestError, "Categories could not be loaded.")); }
    finally { setLoading(false); setRefreshing(false); }
  }, []);
  useEffect(() => { load(); }, [load]);
  return (
    <Screen scroll={false}>
      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        numColumns={2}
        columnWrapperStyle={styles.row}
        contentContainerStyle={styles.list}
        ListHeaderComponent={<Heading eyebrow="THE COLLECTIONS" title="Explore by occasion" description="Browse categories currently available at the boutique." />}
        ListEmptyComponent={loading ? <ActivityIndicator color={theme.colors.accent} /> : error ? <View><Text style={styles.message}>{error}</Text><Button title="Try again" variant="secondary" onPress={load} /></View> : <Text style={styles.message}>No active categories are available yet.</Text>}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={theme.colors.accent} />}
        renderItem={({ item }) => <CategoryCard category={item} onPress={() => navigation.navigate("Costumes", { categoryId: item.id, categoryName: item.name })} />}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  list: { padding: 20, paddingBottom: 38, gap: 16 },
  row: { gap: 12 },
  message: { color: theme.colors.muted, lineHeight: 21, marginVertical: 8 },
});
