import { useEffect, useMemo, useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";
import { Screen, Heading, Notice } from "../components/Screen";
import { Button, CostumeImage, Panel } from "../components/UI";
import { formatPrice } from "../components/CatalogCards";
import { getCostume, getCostumeImages, getCostumeVariants } from "../services/catalogService";
import { getApiErrorMessage } from "../services/apiClient";
import { useAuth } from "../context/AuthContext";
import { theme } from "../constants/theme";

export default function CostumeDetailsScreen({ navigation, route }) {
  const costumeId = route.params?.costumeId;
  const { user } = useAuth();
  const [costume, setCostume] = useState(null);
  const [variants, setVariants] = useState([]);
  const [images, setImages] = useState([]);
  const [selectedImage, setSelectedImage] = useState(null);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.all([getCostume(costumeId), getCostumeVariants(costumeId), getCostumeImages(costumeId)])
      .then(([details, available, costumeImages]) => {
        if (!active) return;
        setCostume(details);
        setVariants(available || []);
        setImages(costumeImages || []);
        setSelectedImage(costumeImages?.find((image) => image.primary) || costumeImages?.[0] || null);
        setSelected(available?.[0] || null);
      })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError, "Costume details could not be loaded.")); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [costumeId]);

  const colors = useMemo(() => [...new Map(variants.map((item) => [item.colorName, item])).values()], [variants]);
  const sizes = variants.filter((item) => item.colorName === selected?.colorName);

  function chooseColor(colorName) {
    const sameSize = variants.find((item) => item.colorName === colorName && item.sizeName === selected?.sizeName);
    setSelected(sameSize || variants.find((item) => item.colorName === colorName) || null);
  }

  function tryOn() {
    if (!selected?.available) return;
    const params = { costumeId, variantId: selected.id };
    if (user?.roles?.includes("CUSTOMER")) navigation.navigate("TryOn", params);
    else navigation.navigate("Login", { returnTo: "TryOn", returnParams: params });
  }

  if (loading) return <Screen><ActivityIndicator color={theme.colors.accent} /><Notice>Loading costume details…</Notice></Screen>;
  if (error || !costume) return <Screen><Heading title="Costume unavailable" /><Notice error>{error || "This costume could not be found."}</Notice><Button title="Back to costumes" variant="secondary" onPress={() => navigation.goBack()} /></Screen>;

  return (
    <Screen>
      <CostumeImage uri={selectedImage?.imageUrl || costume.primaryImageUrl} style={styles.image} />
      {images.length > 1 && <View style={styles.gallery}>
        {images.map((image) => <Pressable key={image.id} onPress={() => setSelectedImage(image)} accessibilityRole="button" accessibilityState={{ selected: selectedImage?.id === image.id }}>
          <CostumeImage uri={image.imageUrl} style={[styles.thumbnail, selectedImage?.id === image.id && styles.thumbnailSelected]} />
        </Pressable>)}
      </View>}
      <Text style={styles.kicker}>{costume.categoryName || "DESIGNER OCCASIONWEAR"}</Text>
      <Heading title={costume.name} description={costume.description || "Contact the boutique for more details about this piece."} />
      <Text style={styles.price}>{formatPrice(selected?.price ?? costume.basePrice)}</Text>
      {variants.length ? (
        <Panel>
          <Text style={styles.label}>Color · {selected?.colorName || "Select a color"}</Text>
          <View style={styles.optionRow}>
            {colors.map((variant) => {
              const isSelected = selected?.colorName === variant.colorName;
              return (
                <Pressable key={variant.colorName} accessibilityRole="button" accessibilityState={{ selected: isSelected }} onPress={() => chooseColor(variant.colorName)} style={[styles.colorOption, isSelected && styles.optionSelected]}>
                  <View style={[styles.swatch, { backgroundColor: /^#[\da-f]{6}$/i.test(variant.colorHexCode || "") ? variant.colorHexCode : "#ddd8cf" }]} />
                  <Text style={styles.optionText}>{variant.colorName}</Text>
                </Pressable>
              );
            })}
          </View>
          <Text style={styles.label}>Size · {selected?.sizeName || "Select a size"}</Text>
          <View style={styles.optionRow}>
            {sizes.map((variant) => (
              <Pressable key={variant.id} disabled={!variant.available} onPress={() => setSelected(variant)} style={[styles.sizeOption, selected?.id === variant.id && styles.optionSelected, !variant.available && styles.unavailable]}>
                <Text style={styles.optionText}>{variant.sizeName}{variant.available ? "" : " · unavailable"}</Text>
              </Pressable>
            ))}
          </View>
          {selected ? <Text style={styles.available}>Available · {selected.colorName} / {selected.sizeName}</Text> : <Notice>No available size options for this color.</Notice>}
          <Text style={styles.apiNote}>The catalog API returns available variants only.</Text>
        </Panel>
      ) : <Panel><Notice>No available color and size combinations for this costume.</Notice></Panel>}
      <Button title="Try this costume" onPress={tryOn} disabled={!selected?.available} />
      <Text style={styles.note}>Photo preparation only. AI try-on processing is not enabled yet.</Text>
      <Button title="Ask the boutique" variant="quiet" onPress={() => navigation.navigate("Contact")} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  image: { width: "100%", height: 360, borderRadius: theme.radius.md },
  gallery: { flexDirection: "row", gap: 8 },
  thumbnail: { width: 58, height: 72, borderRadius: 7, borderWidth: 1, borderColor: theme.colors.border },
  thumbnailSelected: { borderWidth: 2, borderColor: theme.colors.accentDark },
  kicker: { color: theme.colors.accent, fontSize: 10, fontWeight: "700", letterSpacing: 1.5 },
  price: { fontSize: 18, fontWeight: "700", color: theme.colors.ink },
  label: { color: theme.colors.ink, fontSize: 13, fontWeight: "700" },
  optionRow: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  colorOption: { flexDirection: "row", alignItems: "center", gap: 7, borderWidth: 1, borderColor: theme.colors.border, padding: 8, borderRadius: 9 },
  swatch: { width: 18, height: 18, borderRadius: 9, borderWidth: 1, borderColor: theme.colors.border },
  sizeOption: { minWidth: 48, minHeight: 40, alignItems: "center", justifyContent: "center", paddingHorizontal: 10, borderWidth: 1, borderColor: theme.colors.border, borderRadius: 8 },
  optionSelected: { borderColor: theme.colors.accentDark, backgroundColor: "#f5f0e9" },
  optionText: { color: theme.colors.ink, fontSize: 12, fontWeight: "600" },
  unavailable: { opacity: 0.4 },
  available: { color: theme.colors.success, fontSize: 12, fontWeight: "600" },
  apiNote: { color: theme.colors.muted, fontSize: 11 },
  note: { color: theme.colors.muted, textAlign: "center", fontSize: 12 },
});
