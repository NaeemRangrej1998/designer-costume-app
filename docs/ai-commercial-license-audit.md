# Phase 9: Commercial AI License Audit

**Sources checked:** 2026-11-06  
**Scope:** FASHN VTON v1.5 at commit `7c0f10af3f91ad4048fe9729c470a13ef905d25a`, model/dependency licenses, and selected open virtual try-on and human-parsing alternatives.  
**Decision:** **B. Candidate technically promising, but licensing still needs clarification.** FASHN VTON v1.5 + SCHP ATR-18 is the best local, pretrained candidate to investigate, but its complete commercial rights and parser compatibility have not been established. It is not cleared for a commercial proof of concept. Do not enable inference or begin Phase 10.

This is a technical license review, not legal advice. License declarations and upstream documentation are evidence, not a legal opinion or a warranty that a publisher has authority over every training-data or model-weight right.

## Executive summary

- FASHN VTON v1.5's code and try-on checkpoint are declared Apache-2.0. But its pipeline unconditionally imports and constructs FASHN Human Parser, even with `segmentation_free=True`; that parser's model card names NVIDIA's SegFormer license. Section 3.3 limits the work and derivative works to non-commercial research or evaluation. This blocks the current FASHN inference stack for a commercial boutique.
- DWPose and YOLOX are separately declared Apache-2.0 and are not the blocker.
- The closest possible parser replacement found is SCHP ATR-18: its code repository and a Hugging Face model card declare MIT. Its checkpoint provenance/training-data rights still need confirmation, and its class labels do not match FASHN's parser contract. It is **not a verified drop-in**.
- IDM-VTON, CatVTON, and OOTDiffusion explicitly make their code/checkpoints available under CC BY-NC-SA 4.0, so they are not commercial alternatives.
- Community Kolors VTON repositories/model mirrors do not establish an authoritative, complete commercial license chain. The official Kolors repository located is a text-to-image project, not a verified official virtual try-on model.
- The FASHN project documents about 8 GB VRAM and approximately 2 GB for its main weights. Kaggle/Colab may offer free GPU sessions, subject to availability, quotas, and runtime limits. Free execution does not change model licensing.

## 1. Current FASHN dependency and license audit

### Inference components and pretrained artifacts

| Component | Exact source/version or artifact | Declared license | Commercial assessment |
|---|---|---|---|
| VTON code and architecture | [FASHN VTON v1.5, pinned source commit](https://github.com/fashn-AI/fashn-vton-1.5/tree/7c0f10af3f91ad4048fe9729c470a13ef905d25a); package version 1.5.0 | Apache-2.0 in `pyproject.toml` and repository license | Permissive for commercial use, subject to Apache terms and independent component rights |
| Try-on model weights | [`fashn-ai/fashn-vton-1.5` model card](https://huggingface.co/fashn-ai/fashn-vton-1.5); `model.safetensors`, 1,943,668,048 bytes (about 1.81 GiB); model card reports about 972M parameters | Apache-2.0 on the model card | Permissive as declared; does not override third-party restrictions |
| Human parser package/code | [FASHN Human Parser v0.1.1](https://github.com/fashn-AI/fashn-human-parser); package `fashn-human-parser>=0.1.1` | Package license defers to the underlying NVIDIA SegFormer license | **Non-commercial research/evaluation only** |
| Human parser checkpoint | [`fashn-ai/fashn-human-parser` model card](https://huggingface.co/fashn-ai/fashn-human-parser); SegFormer-B4 fine-tuned model, 64,008,402 parameters, repository storage about 256 MB | Model card identifies `nvidia-segformer` and links the [NVIDIA SegFormer license](https://github.com/NVlabs/SegFormer/blob/master/LICENSE) | **Non-commercial research/evaluation only**. NVIDIA license §3.3 limits the work and derivatives to non-commercial use |
| Pose detector | [DWPose repository](https://github.com/IDEA-Research/DWPose); FASHN's [DWPose weight repository](https://huggingface.co/fashn-ai/DWPose) | Apache-2.0 declared in DWPose and Hugging Face model metadata | Permissive as declared |
| Pose weights | `dw-ll_ucoco_384.onnx` (134,399,116 bytes) and `yolox_l.onnx` (216,746,733 bytes), total 351,145,849 bytes | FASHN's DWPose model repository declares Apache-2.0 | Permissive as declared |
| YOLOX component | [YOLOX repository](https://github.com/Megvii-BaseDetection/YOLOX) and the YOLOX ONNX pose-detector artifact above | Apache-2.0 declared upstream | Permissive as declared |

FASHN's upstream [`pipeline.py`](https://github.com/fashn-AI/fashn-vton-1.5/blob/7c0f10af3f91ad4048fe9729c470a13ef905d25a/src/fashn_vton/pipeline.py) imports `FashnHumanParser` and `CATEGORY_TO_BODY_COVERAGE`, constructs the parser in `_setup_hp_model`, and predicts segmentation for both person and garment images on each inference call. The [pinned agnostic preprocessing module](https://github.com/fashn-AI/fashn-vton-1.5/blob/7c0f10af3f91ad4048fe9729c470a13ef905d25a/src/fashn_vton/preprocessing/agnostic.py) consumes parser labels to prepare the person input. This occurs regardless of the `segmentation_free` argument; that flag changes person masking behavior, not parser initialization or prediction. The parser is a required inference dependency, not an optional feature that can be disabled by configuration.

### Parser pipeline and replacement feasibility

The parser output is consumed by FASHN preprocessing to identify garment regions and body coverage and to build the garment/person inputs to the VTON network. It also relies on parser-specific `BODY_COVERAGE_TO_LABELS`, `IDENTITY_LABELS`, and `LABELS_TO_IDS` mappings. In particular, the `segmentation_free` option does not avoid parsing; it only changes how the parsed person map is used.

SCHP ATR-18 is technically promising as an adapter target, but it is **not a drop-in replacement**. It outputs an 18-class semantic map, while FASHN expects its own class semantics and mappings. The published ATR categories include upper-clothes, dress, skirt, pants, left/right arms and legs, shoes, scarf, hat, belt, and bag; ATR does not provide distinct hand or torso classes and has no jewelry class. These differences, including left/right shoe labels versus FASHN's body-part mapping, mean that simply returning SCHP's integer map from a replacement `predict()` would mislabel pixels and break or degrade body/garment masks.

Replacement therefore requires a maintained fork/adapter: map SCHP labels to FASHN's expected semantic IDs; define category body coverage and identity-preservation labels; alter preprocessing for classes ATR does not represent; and validate person and garment masks across all supported categories. No evidence establishes equivalence of the two label maps or quality. Running the released VTON weights would not inherently require retraining, but changed mask/preprocessing distributions may reduce quality, and fine-tuning could become necessary. This compatibility remains untested; no weights were downloaded and no image was generated.

### Direct Python dependencies declared by FASHN

The pinned `pyproject.toml` declares version ranges rather than a fully locked environment. The direct runtime package names and their upstream license identifiers are:

| Direct dependency | Declared range in FASHN | Upstream license reference |
|---|---|---|
| PyTorch (`torch`) | `>=2.0.0` | [BSD-3-Clause](https://github.com/pytorch/pytorch/blob/main/LICENSE) |
| TorchVision | `>=0.15.0` | [BSD-3-Clause](https://github.com/pytorch/vision/blob/main/LICENSE) |
| safetensors | `>=0.3.0` | [Apache-2.0](https://github.com/huggingface/safetensors/blob/main/LICENSE) |
| huggingface_hub | `>=0.20.0` | [Apache-2.0](https://github.com/huggingface/huggingface_hub/blob/main/LICENSE) |
| Pillow | `>=9.0.0` | [Pillow license](https://github.com/python-pillow/Pillow/blob/main/LICENSE) |
| NumPy | `>=1.21.0` | [BSD-3-Clause](https://github.com/numpy/numpy/blob/main/LICENSE.txt) |
| OpenCV Python | `>=4.5.0` | [Apache-2.0](https://github.com/opencv/opencv/blob/4.x/LICENSE) |
| tqdm | `>=4.65.0` | [MPL-2.0 / MIT](https://github.com/tqdm/tqdm/blob/master/LICENCE) |
| einops | `>=0.6.0` | [MIT](https://github.com/arogozhnikov/einops/blob/main/LICENSE) |
| onnxruntime-gpu | `>=1.14.0` | [MIT](https://github.com/microsoft/onnxruntime/blob/main/LICENSE) |
| matplotlib | `>=3.5.0` | [Matplotlib license](https://github.com/matplotlib/matplotlib/blob/main/LICENSE) |
| fashn-human-parser | `>=0.1.1` | See the restricted parser row above; its declared license is not an ordinary permissive package license |
| Transformers (parser dependency) | Declared by the parser; version not pinned by FASHN | [Apache-2.0](https://github.com/huggingface/transformers/blob/main/LICENSE) for the library; this does not change the license of parser weights |

The parser separately declares PyTorch, Transformers, OpenCV, NumPy, and Pillow. The shared dependencies do not remove the parser's license restriction. FASHN's `>=` ranges, optional system/CUDA runtimes, and transitive dependency graphs are not pinned to one reproducible environment, so this audit does not claim to be a complete transitive SBOM. Before shipping any model environment, lock versions and generate a package and model-artifact SBOM/license report.

### Dataset and checkpoint provenance

| Model / dataset | Primary-source evidence | Commercial-use status |
|---|---|---|
| FASHN VTON training data | Model card describes 18M masked try-on pairs and 4M synthetic triplets, but the reviewed materials do not identify all source datasets or their terms | **Unverified.** Apache declarations for code and weights do not establish training-data rights or provenance |
| FASHN Human Parser training data | Parser card identifies a fine-tuned SegFormer-B4 model but does not document full dataset licensing/provenance | **Unverified**, in addition to the parser's explicit non-commercial SegFormer restriction |
| SCHP ATR-18 data/checkpoint | The model card identifies ATR and declares MIT; upstream SCHP code is MIT. The reviewed sources do not establish the ATR dataset terms or the checkpoint uploader's authority to grant rights in the trained weights | **Unverified.** MIT labels alone do not clear dataset or checkpoint rights |
| VITON-HD | [Upstream repository](https://github.com/shadow2496/VITON-HD) says the dataset is for research purposes and indicates CC BY-NC 4.0 | Research-only/non-commercial; not suitable as a commercial data source |
| DressCode | [Upstream repository](https://github.com/aimagelab/dress-code) says the dataset will not be released to private companies | Not available for this boutique's commercial use under the stated distribution terms |

These are separate from model-code licenses. IDM-VTON, CatVTON, and OOTDiffusion are already disqualified by their own non-commercial licenses; use of VITON-HD or DressCode introduces additional dataset restrictions and does not create commercial rights.

### Size and hardware

- Main VTON checkpoint: 1.94 GB decimal (about 1.81 GiB); upstream describes the model weights as approximately 2 GB.
- DWPose/YOLOX ONNX files: approximately 351 MB combined.
- Human parser: approximately 244 MB according to its upstream README; current Hugging Face repository storage is about 256 MB.
- Approximate model artifact total: about 2.55 GB decimal, before Python/CUDA packages, caches, and temporary inference memory.
- Upstream reports roughly **8 GB VRAM** for inference and recommends a dedicated GPU. Actual peak memory depends on GPU, runtime, precision, and input processing.
- Upstream provides pretrained weights and an inference pipeline; no training is required to run the published model.
- The current project has not downloaded these weights and has not run real inference.

## 2. Alternative model and parser comparison

“Free GPU” below means that a user may try a locally downloaded, pre-trained model in an available Kaggle/Colab GPU session. It does not mean a hosted GPU is guaranteed, permanently free, or unrestricted. Notebook quotas and accelerator availability change by account, region, and time.

| Candidate | Repository/version | Main/model license | Parser and pose licenses | Other important components | Commercial status | Approx. VRAM / model download | Garments; training; free GPU |
|---|---|---|---|---|---|---|---|
| FASHN VTON v1.5, official parser | [FASHN v1.5](https://github.com/fashn-AI/fashn-vton-1.5/tree/7c0f10af3f91ad4048fe9729c470a13ef905d25a), package 1.5.0 | Apache-2.0; pretrained checkpoint also declared Apache-2.0 | FASHN Human Parser and its SegFormer-derived weights: NVIDIA SegFormer license, non-commercial research/evaluation only. DWPose/YOLOX: Apache-2.0 | Direct dependencies listed above | **Restricted for this application** because the required parser is non-commercial | About 8 GB VRAM; about 2.55 GB total model/parser/pose artifacts | `tops`, `bottoms`, `one-pieces`; worn-model and flat-lay garment images. Pretrained; no training needed. Can be evaluated on a free GPU session if available, but not commercially used under the current parser license |
| FASHN VTON v1.5 + SCHP ATR parser (conditional research candidate only) | VTON as above; [SCHP ATR-18 model](https://huggingface.co/pirocheto/schp-atr-18), model card revision `61f4ff3c508b899904e495cefbbaff9cacadeb45` | VTON Apache-2.0; SCHP code repository and this model card declare MIT | Candidate SCHP ATR parser declares MIT; retain the [upstream SCHP license](https://github.com/GoGoDuck912/Self-Correction-Human-Parsing/blob/master/LICENSE) and review data/checkpoint provenance. DWPose/YOLOX Apache-2.0 | Requires a custom parser adapter and label mapping; model card demonstrates use of remote custom code (`trust_remote_code=True`), which must be pinned and reviewed | **Unclear—not approved.** The MIT declaration is promising, but ATR training-data/checkpoint rights and the uploader's authority to grant rights have not been independently confirmed | Parser weights: about 67 MB FP32, or about 66–69 MB for INT8 ONNX. End-to-end VRAM remains approximately FASHN's 8 GB; parser can use CPU ONNX | Same FASHN categories and pretrained VTON; no training needed for released checkpoints. GPU notebook potentially usable. **Not a drop-in:** SCHP ATR IDs/semantics differ from FASHN's custom labels (for example ATR lacks separate hands/jewelry labels); FASHN imports parser-specific category mappings. No adapter or generated image has been verified |
| IDM-VTON | [Official repository](https://github.com/yisol/IDM-VTON), Hugging Face revision `585a32e74aee241cbc0d0cc3ab21392ca58c916a` | CC BY-NC-SA 4.0; repository states code and checkpoints use this license | Uses human parsing (ATR/LIP), DensePose and OpenPose assets. Their licenses must be reviewed independently; the IDM-VTON license already disallows commercial use | SDXL inpainting base model, IP-Adapter/image encoder, Diffusers, Transformers, ONNX Runtime, and other listed dependencies | **Restricted** by the project's NC-SA license, regardless of component licenses | Hugging Face model repository reports about 41.4 GB of stored artifacts; upstream does not state a dependable minimum VRAM in the reviewed README. SDXL-family execution is materially heavier than an 8 GB promise | Upper/lower/dress-style VITON-HD and DressCode workflows; released checkpoints, no training needed for inference. Free GPU sessions may be inadequate or quota-limited |
| CatVTON | [Official repository](https://github.com/Zheng-Chong/CatVTON), Hugging Face revision `2969fcf85fe62f2036605716f0b56f0b81d01d79` | CC BY-NC-SA 4.0 explicitly covers code, checkpoints, and demo | SCHP and DensePose are used for mask-generation workflows; component terms are separate but cannot lift the project's NC restriction | Stable Diffusion 1.5 inpainting base model; its model card declares [CreativeML OpenRAIL-M](https://huggingface.co/stable-diffusion-v1-5/stable-diffusion-inpainting). CatVTON's model repository contains about 1.42 GB, with base-model and software requirements in addition | **Restricted** by CC BY-NC-SA | Upstream states **under 8 GB VRAM** at 1024x768 in bf16; 899.06M total parameters. About 1.42 GB in its Hugging Face repository, plus the required base-model/software downloads | Upper-body, lower-body, dresses via VITON-HD/DressCode categories. Pretrained release; no training required for inference. Good fit for trying on a free GPU session, but not for commercial use |
| OOTDiffusion | [Official repository](https://github.com/levihsu/OOTDiffusion), Hugging Face revision `c79f9dd0585743bea82a39261cc09a24040bc4f9` | CC BY-NC-SA 4.0 in repository and model card | SCHP human parsing and OpenPose checkpoints; parser code may say MIT, but does not alter OOTDiffusion's NC-SA terms | CLIP ViT-L/14, PyTorch, ONNX human parsing; project materials report about 19.3 GB total Hugging Face storage | **Restricted** by CC BY-NC-SA | Minimum VRAM not stated in the reviewed official README; around 19.3 GB of repository artifacts (multiple checkpoints) | Half-body upper-body and full-body upper/lower/dress categories, trained/released for VITON-HD and DressCode. Pretrained; no training needed for inference. Free notebook fit is uncertain due model download and unspecified VRAM |
| Kolors virtual try-on community mirrors | No single authoritative upstream VTON repository/version was verified. The official [Kwai-Kolors/Kolors repository](https://github.com/Kwai-Kolors/Kolors) is a text-to-image model project, not an official VTON implementation | Some community mirrors carry Apache tags, but no authoritative complete VTON license chain or original weight provenance was confirmed. Official Kolors code is Apache-2.0; its [model license](https://github.com/Kwai-Kolors/Kolors/blob/master/MODEL_LICENSE) requires registration for commercial use when MAU is at most 300 million and an express license above that threshold | Varies or is undocumented per mirror | VTON code, adapted weights, parser/pose dependencies, and base Kolors model need independent source/license verification | **Unclear for VTON.** Kolors base weights have a conditional commercial pathway, not a blanket Apache permission; no authoritative Kolors VTON stack was verified | Not reliably established | Person/garment input behavior, traditional garment support, and no-training inference depend on the specific unverified mirror. No candidate selected |

### Pretrained human parsing and pose components

| Component | License evidence | Suitability |
|---|---|---|
| FASHN Human Parser / SegFormer | FASHN parser model card links NVIDIA SegFormer terms; NVIDIA license §3.3 says the work and derivatives may only be used non-commercially for research/evaluation | Technically required by FASHN v1.5; not commercially usable under the current published terms |
| SCHP ATR-18 (`pirocheto/schp-atr-18`) | Model card says MIT; original [SCHP repository](https://github.com/GoGoDuck912/Self-Correction-Human-Parsing) is MIT | Best parser candidate found for a separate, gated compatibility investigation. It is pretrained and can run without training. Commercial rights are not confirmed until ATR dataset/checkpoint provenance is cleared. Its labels require adaptation and real-image validation |
| Meta Segment Anything 2 (SAM 2) | [Official repository](https://github.com/facebookresearch/sam2) and checkpoints declare Apache-2.0 | Commercially permissive as declared, but it is promptable object segmentation, not human/clothing semantic parsing. It does not natively emit the per-category labels FASHN expects. Prompt design and additional category/mask-selection logic would be required; not a verified parser replacement |
| BRIA RMBG-1.4 | [Official repository/model card](https://huggingface.co/briaai/RMBG-1.4) states a non-commercial license; commercial use requires a separate BRIA agreement | Not commercially usable without that agreement. It segments foreground/background, not apparel categories, so it cannot directly replace FASHN's parser |
| DWPose and YOLOX for pose | [DWPose code](https://github.com/IDEA-Research/DWPose) and [FASHN-hosted DWPose weights](https://huggingface.co/fashn-ai/DWPose) declare Apache-2.0 | Commercially permissive as declared and already part of FASHN's non-parser stack; it estimates pose, not semantic clothing/body-part parsing |

### Kolors licensing clarification

The official Kolors README describes its code as Apache-2.0 and directs model users to a separate [MODEL_LICENSE](https://github.com/Kwai-Kolors/Kolors/blob/master/MODEL_LICENSE). That model agreement says commercial users at or below the stated 300-million-monthly-active-user threshold must register with the licensor to obtain the corresponding business license; users above the threshold must request a license and receive express approval. The README links a [commercial authorization questionnaire](https://github.com/Kwai-Kolors/Kolors/blob/master/imgs/%E5%8F%AF%E5%9B%BEKOLORS%E6%A8%A1%E5%9E%8B%E5%95%86%E4%B8%9A%E6%8E%88%E6%9D%83%E7%94%B3%E8%AF%B7%E4%B9%A6.docx). The repository's Hugging Face model card tag alone is therefore not sufficient to conclude unrestricted commercial use. Kolors is a text-to-image model; this research did not verify an official Kolors VTON model or the license of any community VTON adapter.

### Third-party components in non-commercial VTON alternatives

| Candidate | Identified additional assets/dependencies | License completeness |
|---|---|---|
| IDM-VTON | [SDXL inpainting](https://huggingface.co/stabilityai/stable-diffusion-xl-1.0-inpainting-0.1), IP-Adapter/image encoder, Diffusers, Transformers, ONNX Runtime, human parsing (ATR/LIP), DensePose and OpenPose assets | The project-level CC BY-NC-SA 4.0 already blocks commercial use. The reviewed project does not pin every auxiliary checkpoint revision and license; each must be reviewed independently for any lawful non-commercial reproduction |
| CatVTON | [Stable Diffusion 1.5 inpainting checkpoint](https://huggingface.co/stable-diffusion-v1-5/stable-diffusion-inpainting) (CreativeML OpenRAIL-M per model card), Diffusers, SCHP, DensePose | CatVTON's own CC BY-NC-SA 4.0 already blocks commercial use. The named base model has a distinct license; parser/pose checkpoints and exact dependency versions require separate review |
| OOTDiffusion | CLIP ViT-L/14, SCHP parsing, OpenPose and ONNX Runtime assets | OOTDiffusion's CC BY-NC-SA 4.0 already blocks commercial use. Exact revisions/terms for all auxiliary checkpoints are not fully established by the reviewed materials |

This is an inventory of components named by the projects, not a fully resolved bill of materials. No alternative above becomes commercially usable by clearing only its auxiliary parser or base model; the VTON project's own non-commercial license remains dispositive.

## 3. Commercial use and license interpretation

1. **FASHN v1.5 overall: blocked under the current stack.** Its permissive main license does not cancel the NVIDIA restriction inherited by the required human parser. The parser is loaded and called even in segmentation-free mode.
2. **CC BY-NC-SA projects: reject for this business.** The `NC` condition does not grant commercial use. Do not rely on an internal research label for a system intended to serve a costume shop.
3. **MIT/Apache declarations are not a complete rights audit.** Confirm the licensor's rights in checkpoints, datasets, and derived weights. Preserve attribution/notices and inspect all separately licensed base models and auxiliary weights.
4. **No lockfile means dependency completeness is not yet provable.** Before distribution or deployment, create a pinned, reproducible environment and SBOM covering Python packages, native/CUDA libraries, model checkpoints, parser assets, pose assets, and their source revisions.
5. **Customer image and dataset rights are separate.** A model license does not grant rights to sample person/garment images or permission to retain/process customers' photos.

## 4. Garment and Indian-fashion suitability

The upstream FASHN VTON v1.5 documentation supports only `tops`, `bottoms`, and `one-pieces`, accepts worn-model or flat-lay garment photos, and documents 576x864 output. It does not establish a dedicated saree, lehenga, choli, dupatta, salwar suit, or regional drape category.

IDM-VTON, CatVTON, and OOTDiffusion document workflows around VITON-HD and/or DressCode categories (upper body, lower body, dresses). That is not evidence of reliable Indian traditional garment rendering. SCHP ATR has labels such as skirt, dress, and scarf but is a parser, not a VTON generator, and does not provide a traditional-garment VTON quality guarantee.

| Garment | Evidence-based support assessment |
|---|---|
| Saree | No reviewed candidate documents saree-specific training or evaluation. Saree drape, pleats, pallu, and blouse are not equivalent to a generic top/dress mask; quality is unknown and likely challenging |
| Lehenga | FASHN has generic `tops`/`bottoms`/`one-pieces`, but no verified coordinated lehenga-choli/dupatta workflow. Separate pieces, layered fabric, and full-length skirt structure are not established |
| Sherwani | Could be treated as a generic top/one-piece depending on framing, but no reviewed model validates sherwani construction, embroidery preservation, or long-coat overlap |
| Blouse | Generic tops support makes it the closest category fit, but blouse-specific cuts, sleeves, back designs, and draping interaction remain unverified |
| Long dress / gown | FASHN's `one-pieces` and alternatives' dress categories provide the clearest nominal category fit. This does not establish Indian gown or regional outfit quality |

The public VITON-HD/DressCode-based evaluations are not evidence of Indian traditional garment capability. Any eventual evaluation must use consented, representative Indian-garment samples and assess garment identity/pattern preservation, drape, color, body preservation, failure modes, and customer safety. Do not market traditional garment support until measured.

## 5. Recommendation and next steps

### Recommendation

**Do not select or enable an inference model for commercial use yet.** No candidate reviewed has a fully verified, commercially permitted end-to-end model + parser + pose + weight + dependency chain together with demonstrated output quality for the boutique's target garments.

The most plausible next **non-production investigation** is the combination **FASHN VTON v1.5 + SCHP ATR-18 parser + existing DWPose/YOLOX**, because the declared VTON and pose licenses are permissive and the alternate parser advertises MIT. This is a hypothesis only—not a recommendation to ship or to download weights now. Its model/data rights are not fully established and its semantic labels are not a drop-in match.

### Gated implementation sequence

1. Keep `MODEL_USE_MODE=disabled`; do not download FASHN or parser weights and do not enable inference for customer requests.
2. Obtain legal review or written rights confirmation for the SCHP ATR checkpoint and its ATR training-data/provenance, plus any other model assets. If that cannot be established, reject it.
3. If rights are cleared, build an isolated, non-production parser adapter. Pin source revisions and add an explicit ATR-to-FASHN label mapping; inspect FASHN's category-to-body-coverage and preprocessing behavior before changing integration.
4. With authorized, consented test images, verify parser masks for person and garment inputs, output dimensions/types, category coverage, and failure handling. Compare masks against FASHN parser outputs only in a legally permitted evaluation context. Do not claim compatibility based solely on both parsers having 18 classes.
5. Generate and review at least one real VTON image on a free Kaggle/Colab GPU if available. Record GPU type, VRAM, peak memory, runtime, model revision, license approvals, and image consent. A mock unit-test pipeline is not a real inference result.
6. If the rights or parser compatibility gates fail, choose a model with a commercially authorized complete dependency/weight chain or obtain a commercial license from the relevant rights holders. Paid inference APIs and purchased GPUs are outside the current project constraints.
7. Only after an explicit model selection and successful real-image inference should Phase 10 proceed. Then pin all model/package artifacts, generate an SBOM, document retention/privacy controls, and implement deployment safeguards.

## Sources

- [FASHN VTON v1.5 pinned repository](https://github.com/fashn-AI/fashn-vton-1.5/tree/7c0f10af3f91ad4048fe9729c470a13ef905d25a)
- [FASHN VTON model card and artifact metadata](https://huggingface.co/fashn-ai/fashn-vton-1.5)
- [FASHN parser source and license notice](https://github.com/fashn-AI/fashn-human-parser)
- [FASHN parser model card and license metadata](https://huggingface.co/fashn-ai/fashn-human-parser)
- [NVIDIA SegFormer license, including §3.3](https://github.com/NVlabs/SegFormer/blob/master/LICENSE)
- [FASHN-hosted DWPose model card and files](https://huggingface.co/fashn-ai/DWPose)
- [DWPose repository](https://github.com/IDEA-Research/DWPose)
- [YOLOX repository and license](https://github.com/Megvii-BaseDetection/YOLOX)
- [SCHP upstream code and MIT license](https://github.com/GoGoDuck912/Self-Correction-Human-Parsing)
- [SCHP ATR-18 model card and license](https://huggingface.co/pirocheto/schp-atr-18)
- [Meta SAM 2 repository and license](https://github.com/facebookresearch/sam2)
- [BRIA RMBG-1.4 model card and license](https://huggingface.co/briaai/RMBG-1.4)
- [IDM-VTON repository](https://github.com/yisol/IDM-VTON), [model card](https://huggingface.co/yisol/IDM-VTON), and [CC BY-NC-SA 4.0 legal code](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode)
- [CatVTON repository](https://github.com/Zheng-Chong/CatVTON), [model card](https://huggingface.co/zhengchong/CatVTON), and its [license](https://github.com/Zheng-Chong/CatVTON/blob/main/LICENSE)
- [OOTDiffusion repository](https://github.com/levihsu/OOTDiffusion) and [model card](https://huggingface.co/levihsu/OOTDiffusion)
- [Official Kolors repository](https://github.com/Kwai-Kolors/Kolors)
- [VITON-HD dataset terms](https://github.com/shadow2496/VITON-HD) and [DressCode dataset terms](https://github.com/aimagelab/dress-code)
