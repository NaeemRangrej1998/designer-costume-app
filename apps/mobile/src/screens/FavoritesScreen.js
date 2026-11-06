import { Screen, Heading } from "../components/Screen";
import { Button, Panel } from "../components/UI";
import { useAuth } from "../context/AuthContext";
import { theme } from "../constants/theme";
import { Text } from "react-native";

export default function FavoritesScreen({ navigation }) {
  const { user } = useAuth();
  if (!user?.roles?.includes("CUSTOMER")) {
    return <Screen><Heading eyebrow="YOUR BOUTIQUE EDIT" title="Favorites" description="Sign in to see whether saved pieces are available." /><Button title="Sign in" onPress={() => navigation.navigate("Login")} /></Screen>;
  }
  return (
    <Screen>
      <Heading eyebrow="YOUR BOUTIQUE EDIT" title="Favorites" description="Keep the looks you love close by." />
      <Panel>
        <Text style={{ color: theme.colors.accent, fontSize: 30 }}>♡</Text>
        <Text style={{ color: theme.colors.ink, fontSize: 18, fontWeight: "700" }}>Favorites are not supported yet</Text>
        <Text style={{ color: theme.colors.muted, lineHeight: 21 }}>The backend has no favorites endpoints. Nothing is being stored locally or presented as permanently saved.</Text>
        <Button title="Browse costumes" onPress={() => navigation.navigate("Costumes")} />
      </Panel>
    </Screen>
  );
}
