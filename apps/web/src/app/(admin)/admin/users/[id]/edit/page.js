import UserForm from "@/components/admin/UserForm";

export default async function EditUserPage({ params }) {
  const { id } = await params;
  return <UserForm userId={id} />;
}
