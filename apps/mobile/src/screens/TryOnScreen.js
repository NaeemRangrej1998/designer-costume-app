import { useEffect, useState } from "react";
import { ActivityIndicator, Image, StyleSheet, Text } from "react-native";
import * as ImagePicker from "expo-image-picker";
import { Screen, Heading, Notice } from "../components/Screen";
import { Button, Panel } from "../components/UI";
import { getCostume, getCostumeVariants } from "../services/catalogService";
import { getAccessToken, getApiBaseUrl, getApiErrorMessage } from "../services/apiClient";
import { deleteCustomerPhoto, getCustomerPhotos, uploadCustomerPhoto } from "../services/customerPhotosService";
import { useAuth } from "../context/AuthContext";
import { theme } from "../constants/theme";

const allowedTypes = ["image/jpeg", "image/png", "image/webp"];

export default function TryOnScreen({ navigation, route }) {
  const { user } = useAuth();
  const { costumeId, variantId } = route.params || {};
  const [costume, setCostume] = useState(null);
  const [variant, setVariant] = useState(null);
  const [image, setImage] = useState(null);
  const [loading, setLoading] = useState(Boolean(costumeId));
  const [error, setError] = useState("");
  const [photoError, setPhotoError] = useState("");
  const [photoMessage, setPhotoMessage] = useState("");
  const [photos, setPhotos] = useState([]);
  const [photoBusy, setPhotoBusy] = useState(false);

  useEffect(() => {
    if (!user?.roles?.includes("CUSTOMER")) return undefined;
    let active = true;
    getCustomerPhotos()
      .then((items) => { if (active) setPhotos(items); })
      .catch((requestError) => { if (active) setPhotoError(getApiErrorMessage(requestError, "Your private photo library could not be loaded.")); });
    return () => { active = false; };
  }, [user]);

  useEffect(() => {
    if (!user?.roles?.includes("CUSTOMER")) return;
    if (!costumeId) { setLoading(false); return; }
    let active = true;
    Promise.all([getCostume(costumeId), getCostumeVariants(costumeId)])
      .then(([details, variants]) => {
        if (!active) return;
        setCostume(details);
        setVariant(variants.find((item) => item.id === variantId && item.available) || null);
      })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError, "The selected look could not be loaded.")); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user, costumeId, variantId]);

  async function pickImage() {
    setPhotoError("");
    setPhotoMessage("");
    try {
      const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!permission.granted) {
        setPhotoError("Allow photo library access to choose an image. The image remains on this device.");
        return;
      }
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ["images"],
        allowsEditing: true,
        quality: 0.9,
      });
      if (result.canceled) return;
      const asset = result.assets?.[0];
      if (!asset) return;
      const uri = asset.uri.toLowerCase();
      const mimeType = asset.mimeType || (uri.endsWith(".png") ? "image/png" : uri.endsWith(".webp") ? "image/webp" : "image/jpeg");
      if (!allowedTypes.includes(mimeType)) {
        setPhotoError("Choose a JPEG, PNG, or WebP photo.");
        return;
      }
      if (asset.fileSize > 10 * 1024 * 1024) {
        setPhotoError("Choose a photo smaller than 10 MB.");
        return;
      }
      setImage({ uri: asset.uri, mimeType, fileName: asset.fileName });
    } catch (requestError) {
      setPhotoError(requestError.message || "The photo library could not be opened.");
    }
  }

  async function savePhoto() {
    if (!image?.uri || image.savedPhotoId) return;
    setPhotoBusy(true);
    setPhotoError("");
    setPhotoMessage("");
    try {
      const savedPhoto = await uploadCustomerPhoto({
        uri: image.uri,
        mimeType: image.mimeType,
        fileName: image.fileName || image.uri.split("/").pop(),
      });
      setImage((current) => current ? { ...current, savedPhotoId: savedPhoto.id } : current);
      setPhotos(await getCustomerPhotos());
      setPhotoMessage("Photo saved to your private account library.");
    } catch (requestError) {
      setPhotoError(getApiErrorMessage(requestError, "The photo could not be saved."));
    } finally {
      setPhotoBusy(false);
    }
  }

  async function removePhoto(photoId) {
    setPhotoBusy(true);
    setPhotoError("");
    setPhotoMessage("");
    try {
      await deleteCustomerPhoto(photoId);
      setPhotos((current) => current.filter((photo) => photo.id !== photoId));
      setPhotoMessage("Photo deleted from your private account library.");
      if (image?.savedPhotoId === photoId) setImage(null);
    } catch (requestError) {
      setPhotoError(getApiErrorMessage(requestError, "The photo could not be deleted."));
    } finally {
      setPhotoBusy(false);
    }
  }

  function chooseSavedPhoto(photo) {
    const url = new URL(photo.contentUrl, `${getApiBaseUrl().replace(/\/+$/, "")}/`).toString();
    setImage({
      uri: url,
      mimeType: photo.mimeType,
      fileName: photo.originalFileName,
      savedPhotoId: photo.id,
      headers: { Authorization: `Bearer ${getAccessToken()}` },
    });
    setPhotoError("");
    setPhotoMessage("");
  }

  if (!user?.roles?.includes("CUSTOMER")) {
    return <Screen><Heading eyebrow="CUSTOMER ACCOUNT" title="Sign in to prepare your photo" description="Try-on preparation is available to customer accounts." /><Button title="Sign in" onPress={() => navigation.navigate("Login", { returnTo: "TryOn", returnParams: route.params })} /><Button title="Create account" variant="secondary" onPress={() => navigation.navigate("Register", { returnTo: "TryOn", returnParams: route.params })} /></Screen>;
  }
  if (loading) return <Screen><ActivityIndicator color={theme.colors.accent} /><Notice>Loading selected costume…</Notice></Screen>;
  if (error) return <Screen><Heading title="Could not load the selected look" /><Notice error>{error}</Notice><Button title="Try again" variant="secondary" onPress={() => navigation.replace("TryOn", route.params)} /></Screen>;

  return (
    <Screen>
      <Heading eyebrow="A NEW WAY TO EXPLORE" title="Try-on preparation" description="Prepare a local photo and available costume. AI processing is not enabled yet." />
      {costume ? <Panel><Text style={styles.selectedLabel}>SELECTED COSTUME</Text><Text style={styles.name}>{costume.name}</Text><Text style={styles.body}>{variant ? variant.colorName + " · " + variant.sizeName : "No available variant selected"}</Text></Panel> : <Panel><Text style={styles.name}>Choose a costume and variant first</Text><Button title="Browse costumes" onPress={() => navigation.navigate("MainTabs", { screen: "Costumes" })} /></Panel>}
      {image ? (
        <>
          <Image source={{ uri: image.uri, headers: image.headers }} style={styles.preview} resizeMode="contain" />
          <Button title="Change photo" variant="secondary" onPress={pickImage} />
          <Button title="Remove photo" variant="quiet" onPress={() => setImage(null)} />
          {image.savedPhotoId ? null : <Button title={photoBusy ? "Saving photo…" : "Save to my private photo library"} onPress={savePhoto} disabled={photoBusy} />}
        </>
      ) : <Button title="Choose a photo" onPress={pickImage} />}
      {photoError ? <Notice error>{photoError}</Notice> : null}
      {photoMessage ? <Notice>{photoMessage}</Notice> : null}
      {photos.length > 0 && <Panel>
        <Text style={styles.name}>Your private photos</Text>
        {photos.map((photo) => <Panel key={photo.id}>
          <Text style={styles.body}>{photo.originalFileName} · {(photo.fileSize / (1024 * 1024)).toFixed(1)} MB</Text>
          <Button title="Use photo" variant="secondary" onPress={() => chooseSavedPhoto(photo)} />
          <Button title={photoBusy ? "Please wait…" : "Delete"} variant="quiet" onPress={() => removePhoto(photo.id)} disabled={photoBusy} />
        </Panel>)}
      </Panel>}
      <Panel>
        <Text style={styles.name}>Photo guidance</Text>
        <Text style={styles.body}>Choose a clear, well-lit full-length photo. Stand naturally with your outfit visible and a simple background. JPEG, PNG, or WebP images are supported.</Text>
      </Panel>
      <Panel style={styles.privacy}>
        <Text style={styles.name}>Privacy notice</Text>
        <Text style={styles.body}>Photos stay on this device unless you explicitly save one to your private, account-only library. Saved photos are not sent to the AI service. Virtual try-on processing will be added in a later phase.</Text>
      </Panel>
    </Screen>
  );
}

const styles = StyleSheet.create({
  selectedLabel: { color: theme.colors.accent, fontSize: 10, letterSpacing: 1.4, fontWeight: "700" },
  name: { color: theme.colors.ink, fontSize: 16, fontWeight: "700" },
  body: { color: theme.colors.muted, fontSize: 13, lineHeight: 20 },
  preview: { width: "100%", height: 400, backgroundColor: theme.colors.pale, borderRadius: theme.radius.md },
  privacy: { backgroundColor: "#efe9df" },
});
