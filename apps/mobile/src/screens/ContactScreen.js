import { Linking, Pressable, Text } from "react-native";
import { Screen, Heading, Notice } from "../components/Screen";
import { Button, Field, Panel } from "../components/UI";
import { theme } from "../constants/theme";

export default function ContactScreen() {
  const address = process.env.EXPO_PUBLIC_SHOP_ADDRESS;
  const phone = process.env.EXPO_PUBLIC_SHOP_PHONE;
  const email = process.env.EXPO_PUBLIC_SHOP_EMAIL;
  return (
    <Screen>
      <Heading eyebrow="WE’RE HERE TO HELP" title="Contact the boutique" description="For personal styling, sizing questions, or details about a costume." />
      <Panel>
        <Text style={{ color: theme.colors.ink, fontWeight: "700", fontSize: 17 }}>Designer Costume Studio</Text>
        <Text style={{ color: theme.colors.muted }}>Address · {address || "Not configured"}</Text>
        {phone
          ? <Pressable onPress={() => Linking.openURL("tel:" + phone)}><Text style={{ color: theme.colors.accentDark }}>Phone · {phone}</Text></Pressable>
          : <Text style={{ color: theme.colors.muted }}>Phone · Not configured</Text>}
        {email
          ? <Pressable onPress={() => Linking.openURL("mailto:" + email)}><Text style={{ color: theme.colors.accentDark }}>Email · {email}</Text></Pressable>
          : <Text style={{ color: theme.colors.muted }}>Email · Not configured</Text>}
      </Panel>
      <Panel>
        <Text style={{ color: theme.colors.ink, fontSize: 17, fontWeight: "700" }}>Send a message</Text>
        <Field label="Name" editable={false} placeholder="Your name" />
        <Field label="Email" editable={false} placeholder="Email address" />
        <Field label="Phone" editable={false} placeholder="Phone number" />
        <Field label="Message" editable={false} placeholder="How can we help?" multiline />
        <Button title="Inquiry submission unavailable" disabled />
        <Notice>The backend has no customer inquiry submission API. This form does not send or save messages.</Notice>
      </Panel>
    </Screen>
  );
}
