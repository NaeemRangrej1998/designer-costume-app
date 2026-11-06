import { Pressable, StyleSheet, Text, View } from "react-native";
import { CostumeImage } from "./UI";
import { theme } from "../constants/theme";

export function formatPrice(price) {
  if (price == null || price === "") return "Price on request";
  const value = Number(price);
  return Number.isFinite(value)
    ? new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value) + " · currency unspecified"
    : "Price on request";
}

export function CategoryCard({ category, onPress, horizontal = false }) {
  return (
    <Pressable onPress={onPress} style={[styles.categoryCard, horizontal && styles.categoryHorizontal]}>
      <CostumeImage uri={category.imageUrl} style={styles.categoryImage} />
      <View style={styles.cardCopy}>
        <Text style={styles.cardTitle}>{category.name}</Text>
        <Text style={styles.cardAction}>Explore collection  →</Text>
      </View>
    </Pressable>
  );
}

export function CostumeCard({ costume, onPress, horizontal = false }) {
  return (
    <Pressable onPress={onPress} style={[styles.costumeCard, horizontal && styles.costumeHorizontal]}>
      <CostumeImage uri={costume.primaryImageUrl} style={styles.costumeImage} />
      <View style={styles.cardCopy}>
        <Text style={styles.kicker}>{costume.categoryName || "DESIGNER OCCASIONWEAR"}</Text>
        <Text style={styles.cardTitle} numberOfLines={2}>{costume.name}</Text>
        <Text style={styles.price}>{formatPrice(costume.basePrice)}</Text>
        <Text style={styles.cardAction}>View details  →</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  categoryCard: { flex: 1, minWidth: 140, overflow: "hidden", borderRadius: theme.radius.md, backgroundColor: theme.colors.surface, borderWidth: 1, borderColor: theme.colors.border },
  categoryHorizontal: { width: 215, flex: 0 },
  categoryImage: { width: "100%", height: 150 },
  costumeCard: { flex: 1, minWidth: 150, overflow: "hidden", borderRadius: theme.radius.md, backgroundColor: theme.colors.surface, borderWidth: 1, borderColor: theme.colors.border },
  costumeHorizontal: { width: 215, flex: 0 },
  costumeImage: { width: "100%", height: 210 },
  cardCopy: { padding: 13, gap: 7 },
  cardTitle: { color: theme.colors.ink, fontSize: 16, fontWeight: "700" },
  cardAction: { color: theme.colors.accentDark, fontSize: 12, fontWeight: "700", marginTop: 2 },
  kicker: { color: theme.colors.accent, fontSize: 9, fontWeight: "700", letterSpacing: 1 },
  price: { color: theme.colors.ink, fontSize: 12, lineHeight: 18 },
});
