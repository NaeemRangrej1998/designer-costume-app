import CostumeCard from "./CostumeCard";

export default function CostumeGrid({ costumes }) {
  return (
    <div className="store-costume-grid">
      {costumes.map((costume) => <CostumeCard costume={costume} key={costume.id} />)}
    </div>
  );
}
