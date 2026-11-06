import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { ActivityIndicator, Text, View } from "react-native";
import { useAuth } from "../context/AuthContext";
import { theme } from "../constants/theme";
import CategoriesScreen from "../screens/CategoriesScreen";
import ContactScreen from "../screens/ContactScreen";
import CostumeDetailsScreen from "../screens/CostumeDetailsScreen";
import CostumesScreen from "../screens/CostumesScreen";
import FavoritesScreen from "../screens/FavoritesScreen";
import HomeScreen from "../screens/HomeScreen";
import { ForgotPasswordScreen, LoginScreen, RegisterScreen } from "../screens/AuthScreens";
import ProfileScreen from "../screens/ProfileScreen";
import TryOnScreen from "../screens/TryOnScreen";

const Stack = createNativeStackNavigator();
const Tabs = createBottomTabNavigator();

function MainTabs() {
  const { status } = useAuth();
  if (status === "loading") {
    return <View style={{ flex: 1, alignItems: "center", justifyContent: "center", backgroundColor: theme.colors.background }}><ActivityIndicator color={theme.colors.accent} /></View>;
  }
  return (
    <Tabs.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: theme.colors.accentDark,
        tabBarInactiveTintColor: theme.colors.muted,
        tabBarStyle: { height: 62, paddingBottom: 8, paddingTop: 6, backgroundColor: "#fff", borderTopColor: theme.colors.border },
        tabBarLabelStyle: { fontSize: 10, fontWeight: "600" },
        tabBarIcon: ({ color }) => <Text style={{ color, fontSize: 18 }}>{({ Home: "⌂", Categories: "◇", Costumes: "♧", Favorites: "♡", Profile: "○" })[route.name]}</Text>,
      })}
    >
      <Tabs.Screen name="Home" component={HomeScreen} />
      <Tabs.Screen name="Categories" component={CategoriesScreen} />
      <Tabs.Screen name="Costumes" component={CostumesScreen} />
      <Tabs.Screen name="Favorites" component={FavoritesScreen} />
      <Tabs.Screen name="Profile" component={ProfileScreen} />
    </Tabs.Navigator>
  );
}

export default function RootNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: theme.colors.background },
        headerTintColor: theme.colors.ink,
        headerTitleStyle: { fontWeight: "700" },
        contentStyle: { backgroundColor: theme.colors.background },
      }}
    >
      <Stack.Screen name="MainTabs" component={MainTabs} options={{ headerShown: false }} />
      <Stack.Screen name="CostumeDetails" component={CostumeDetailsScreen} options={{ title: "Costume details" }} />
      <Stack.Screen name="TryOn" component={TryOnScreen} options={{ title: "Try-on preparation" }} />
      <Stack.Screen name="Contact" component={ContactScreen} options={{ title: "Contact the boutique" }} />
      <Stack.Screen name="Login" component={LoginScreen} options={{ title: "Sign in", presentation: "modal" }} />
      <Stack.Screen name="Register" component={RegisterScreen} options={{ title: "Create account", presentation: "modal" }} />
      <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} options={{ title: "Password recovery", presentation: "modal" }} />
    </Stack.Navigator>
  );
}
