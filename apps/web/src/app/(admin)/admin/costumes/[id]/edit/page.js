import CostumeForm from "@/components/admin/CostumeForm";

export default async function EditCostumePage({ params }) {
  const { id } = await params;
  return <CostumeForm costumeId={id} />;
}
