import CategoryForm from "@/components/admin/CategoryForm";

export default async function EditCategoryPage({ params }) {
  const { id } = await params;
  return <CategoryForm categoryId={id} />;
}
