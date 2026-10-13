# Glitch for ADHD and fast-drifting attention: evidence review

Prepared October 2026 for product planning. It builds on [`store/marketing/research.md`](../store/marketing/research.md) (cited below as **R#n**, e.g. R#7 = Grüning et al. 2023 / one sec) and does not repeat it.

**How sources were checked.** Every source below was opened. "Full text" means we read the methods and results. "Abstract" means we only saw the abstract or the publisher/index record (Europe PMC, Crossref, Semantic Scholar, arXiv listing). "Secondary" means the primary text was blocked to us and the numbers come from reporting about it; those rows say so. No number in this file was filled in from memory.

**Scope reminder.** Today AGENTS.md rules out prompts, blockers, timers, usage tracking, accessibility and usage-access permissions, and any claim about focus. Most ideas below would change that scope and need the owner's explicit approval. This file is evidence, not a decision.

---

## Bottom line (10 lines)

1. ADHD and problem phone use go together, in both directions and with modest effects: longitudinal studies show reciprocal links (Thorell 2024), and frequent digital media use predicted new ADHD symptoms with OR ≈ 1.10 per activity (Ra 2018). None of this shows phones *cause* ADHD.
2. The ADHD traits that matter most for app design are measured and real: steeper delay discounting (d = 0.43, 25 comparisons) and less accurate time perception (Hedges' g > 0.40). "Now" wins over "later", and minutes disappear.
3. Interruptions cost attention even in people without ADHD: a week of notifications on raised self-reported inattention (d = .44) and hyperactivity (d = .45) (Kushlev 2016). Glitch must not become one more interrupter.
4. The best-supported friction ingredient is a **clear, one-tap choice not to open**, not a message: in one sec's experiment the dismiss option had the strongest effect and the reflective message alone did nothing (R#7).
5. Effects fade. Pop-ups lose force within days and within a minute if ignored, and warnings habituate after the second exposure. Varying the form of a reminder helps it last.
6. We found **no controlled trial of any phone self-control tool in people with ADHD**. The ADHD-specific evidence is lab work on if-then plans in children (Gawrilow & Gollwitzer 2008) and qualitative interviews (Hariadi 2026).
7. What ADHD users report: rigid timers and blockers get reset or switched off, hyperfocus is valued, and tools can deepen shame. Flexible, kind, reversible tools fit better. Punitive streaks reduce engagement once broken.
8. For more impulsive people, an **explicit** interrupt worked better than subtle friction (Meinhardt 2026). That argues for clear, honest prompts over clever hidden friction.
9. Short-form video use correlates with worse self-reported attention (r = −.38) and inhibitory control (r = −.41) across 71 studies. A "reels detector" fits a real problem, but on Android it needs the AccessibilityService, which our current privacy rules forbid.
10. Claims: we may describe what Glitch does. We may never say it treats, reduces or helps ADHD or its symptoms. FDA classifies "software intended to provide therapy for ADHD or any of its individual symptoms" as a Class II device (EndeavorRx).

---

## 1. What the research says about ADHD, distraction and phones

- **Direction of the link.** Thorell et al.'s systematic review of 28 longitudinal studies found reciprocal associations: children with ADHD symptoms were more likely to develop high or problematic media use, and media use was linked to later symptoms. Links were more consistent for *problematic* use than for raw screen time. In the largest single cohort (Ra 2018, n = 2,587), each extra high-frequency digital activity was associated with an adjusted OR of 1.10 for later ADHD symptoms. The authors call this "modest" and not proven causal.
- **Size of the overlap.** In 4,512 Korean adolescents the ADHD group had OR 6.43 for smartphone addiction (Kim 2019, cross-sectional). A 2026 meta-analysis of 22 studies found internet addiction associated with ADHD (pooled OR 2.78; abstract only). In 273 adults, ADHD self-report scores correlated with two problem-phone-use scales, with inattention as the predictor (Panagiotidi & Overton 2020). We searched and found **no meta-analysis pooling ADHD and problematic *smartphone* use specifically**.
- **Impulsivity and reward sensitivity.** Delay discounting is higher in ADHD (d = 0.43, k = 25, N = 3,913, low heterogeneity, no detectable publication bias; Jackson & MacKillop 2016). Design reading: a small immediate reward (the feed) beats a larger later one (finishing the task), so a reminder that only says "later you'll be glad" is weak against it.
- **Time blindness.** Children and adolescents with ADHD perceive time less accurately and less precisely and tend to overestimate it (27 studies; Zheng et al. 2022). Design reading: make elapsed time *visible* instead of expecting people to feel it. Monge Roffarello, Lukoff & De Russis (2023) list "Time Fog", interfaces that hide the passing of time, as an attention-capture damaging pattern.
- **Interruptions and task switching.** Notifications alone, even unanswered, disrupt an attention task (Stothart 2015). Interrupted workers finish faster but with more stress, frustration and time pressure (Mark 2008). Short-video viewing specifically impaired remembering and carrying out a planned action, while Twitter and YouTube did not (Chiossi 2023, n = 60).
- **Short-form video.** See section 6.

---

## 2. Evidence table

Relevance: **High** = tests something Glitch could build, in a relevant population; **Medium** = a related mechanism or an ADHD-relevant trait; **Low** = background. "Durability" = whether effects were followed after the intervention ended or over time.

### 2a. ADHD, attention and phone use (the problem)

| # | Citation + link | n | Population | Effect | Durability | Harms | Relevance to Glitch |
|---|---|---|---|---|---|---|---|
| A1 | Thorell, L. B., Burén, J., Ström Wiman, J., Sandberg, D., & Nutley, S. B. (2024). Longitudinal associations between digital media use and ADHD symptoms in children and adolescents: a systematic literature review. *Eur Child Adolesc Psychiatry*, 33(8), 2503–2526. [doi:10.1007/s00787-022-02130-3](https://doi.org/10.1007/s00787-022-02130-3) (abstract) | 28 studies | Ages 0–17 | Reciprocal associations; stronger for problematic use than for screen time. | Longitudinal by design | — | Medium: supports "ADHD and problem use reinforce each other"; no causal or adult data |
| A2 | Ra, C. K., et al. (2018). Association of digital media use with subsequent symptoms of ADHD among adolescents. *JAMA*, 320(3), 255–263. [doi:10.1001/jama.2018.8931](https://doi.org/10.1001/jama.2018.8931) (abstract) | 2,587 | LA 10th graders without ADHD symptoms at baseline, 24 months | Adjusted OR 1.10 (1.05–1.15) per additional high-frequency activity; 4.6% vs 10.5% symptom rate at 0 vs 14 activities | 2-year follow-up | — | Low/Medium: modest, observational, self-report |
| A3 | Kim, S.-G., et al. (2019). The relationship between smartphone addiction and symptoms of depression, anxiety, and ADHD in South Korean adolescents. *Ann Gen Psychiatry*, 18, 1. [doi:10.1186/s12991-019-0224-8](https://doi.org/10.1186/s12991-019-0224-8) (abstract) | 4,512 | Korean middle/high school students | OR 6.43 (4.60–9.00) for smartphone addiction in the ADHD-symptom group | Cross-sectional | — | Medium: the target group is over-represented among heavy users |
| A4 | Panagiotidi, M., & Overton, P. (2022; online 2020). Attention deficit hyperactivity symptoms predict problematic mobile phone use. *Current Psychology*, 41(5), 2765–2771. [doi:10.1007/s12144-020-00785-2](https://doi.org/10.1007/s12144-020-00785-2) (abstract) | 273 | Adult volunteers | ASRS correlated positively with MPPUS and SAS; inattention and younger age predicted risk (r values not in abstract) | Cross-sectional | — | Medium: adult evidence; inattention, not hyperactivity, is the driver |
| A5 | Kılınçel, Ş., Bulut, F., & Kılınçel, O. (2026). The relationship between internet addiction and psychiatric disorders in children and adolescents: a meta-analysis. *BMC Psychology*, 14. [doi:10.1186/s40359-026-05010-6](https://doi.org/10.1186/s40359-026-05010-6) (abstract) | 22 studies, 36,432 | Children/adolescents | Internet addiction associated with ADHD, pooled OR 2.78 (2.09–3.69); heterogeneity "moderate to high" | Mostly cross-sectional | — | Low/Medium: internet, not phone; ADHD-specific k not given |
| A6 | Jackson, J. N. S., & MacKillop, J. (2016). ADHD and monetary delay discounting: a meta-analysis of case-control studies. *Biol Psychiatry CNNI*, 1(4), 316–325. [doi:10.1016/j.bpsc.2016.01.007](https://doi.org/10.1016/j.bpsc.2016.01.007) (full text, [PMC5049699](https://pmc.ncbi.nlm.nih.gov/articles/PMC5049699)) | 25 comparisons, 3,913 | Children and adults | d = 0.43; I² = 7.6%; no age moderation | Trait | — | Medium: "now beats later" is a stable ADHD trait; reminders must work in the moment |
| A7 | Zheng, Q., Wang, X., Chiu, K. Y., & Shum, K. K.-M. (2022; online 2020). Time perception deficits in children and adolescents with ADHD: a meta-analysis. *J Atten Disord*, 26(2), 267–281. [doi:10.1177/1087054720978557](https://doi.org/10.1177/1087054720978557) (abstract) | 27 studies | Ages ≤ 18 | Less accurate (g > 0.40) and less precise (g = 0.66) time perception; overestimation | Trait | — | High for design: supports visible, factual time cues |
| A8 | Kushlev, K., Proulx, J., & Dunn, E. W. (2016). "Silence your phones": smartphone notifications increase inattention and hyperactivity symptoms. *CHI '16*, 1011–1020. [doi:10.1145/2858036.2858359](https://doi.org/10.1145/2858036.2858359) (full text) | 221 (190 in main test) | UBC students, general pool (not ADHD sample) | Inattention 2.38 vs 2.27, d = .44; hyperactivity 2.11 vs 2.01, d = .45 (alerts on vs off, within-person) | 1 week per condition | Inattention linked to lower productivity and well-being | High: Glitch must not add interruptions; raw differences are small on the scale |
| A9 | Stothart, C., Mitchum, A., & Yehnert, C. (2015). The attentional cost of receiving a cell phone notification. *JEP:HPP*, 41(4), 893–897. [doi:10.1037/xhp0000100](https://doi.org/10.1037/xhp0000100) (abstract) | Not in abstract | Lab | Notification alone disrupted an attention task, comparable to using the phone | Single session | — | Medium |
| A10 | Mark, G., Gudith, D., & Klocke, U. (2008). The cost of interrupted work: more speed and stress. *CHI '08*, 107–110. [doi:10.1145/1357054.1357072](https://doi.org/10.1145/1357054.1357072) (abstract via search/ACM listing) | Not checked | Lab office task | Interrupted people worked faster with more stress, frustration, time pressure, effort | Single session | Stress | Low: background on switching cost |
| A11 | Fitz, N., Kushlev, K., Jagannathan, R., Lewis, T., Paliwal, D., & Ariely, D. (2019). Batching smartphone notifications can improve well-being. *Computers in Human Behavior*, 101, 84–94. [doi:10.1016/j.chb.2019.07.016](https://doi.org/10.1016/j.chb.2019.07.016) (abstract) | 237 | Adults, randomized field experiment | Batching 3×/day: more attentive, productive, better mood, more in control, less stress vs control | Duration not stated in the record we saw | **No notifications at all → higher anxiety and FoMO** | Medium: batching beats silence; Glitch can't batch without a permission, but can point users to system settings |
| A12 | Chiossi, F., Haliburton, L., Ou, C., Butz, A., & Schmidt, A. (2023). Short-form videos degrade our capacity to retain intentions. *CHI '23*. [doi:10.1145/3544548.3580778](https://doi.org/10.1145/3544548.3580778) ([arXiv](https://arxiv.org/abs/2302.03714)) (abstract) | 60 | Lab, between-subjects | TikTok, but not Twitter/YouTube/no activity, significantly hurt prospective memory | Single session | — | High for reels detector: "I'll stop after this one" is exactly what gets lost |
| A13 | Nguyen, L., et al. (2025). Feeds, feelings, and focus: a systematic review and meta-analysis of the cognitive and mental health correlates of short-form video use. *Psychological Bulletin*, 151(9), 1125–1146. [doi:10.1037/bul0000498](https://doi.org/10.1037/bul0000498) (abstract) | 71 studies, 98,299 | Youth and adults | Cognition r = −.34; attention r = −.38; inhibitory control r = −.41; mental health r = −.21 (stress −.34, anxiety −.33) | Mostly cross-sectional | — | High for reels detector; correlational, mostly self-report |

### 2b. Interventions

| # | Citation + link | n | Population | Effect | Durability | Harms | Relevance to Glitch |
|---|---|---|---|---|---|---|---|
| B1 | Grüning, D. J., Riedel, F., & Lorenz-Spreen, P. (2023). Directing smartphone use through the self-nudge app one sec. *PNAS*, 120(8), e2213114120. [doi:10.1073/pnas.2213114120](https://doi.org/10.1073/pnas.2213114120) (full text, [PMC9974409](https://pmc.ncbi.nlm.nih.gov/articles/PMC9974409/)). Extends R#7. | 280 field (of 719 starters) + 500 online | Self-selected one sec users; online adults | Online: overall d = 0.72; **dismiss option strongest** (vs friction only d = 0.44); **message alone not effective**. Field: dismissal rate 43% (wk 1) → 36% → 33% → **32–34% plateau** wks 3–6; opening attempts 166 → 105/week | 6 weeks; no follow-up after removal | **61% of new users did not complete 6 weeks**; dropouts saw their use as less problematic | High: the core template for a pause screen. Authors say the generic message was likely why it failed and suggest personalized messages. Developer co-author |
| B2 | Haliburton et al. (2024), R#8 | 1,039 | one sec users | Frictions reduced attempts over time | ~13 weeks | **Users paused the tool and relapsed quickly** | High: design for "pause" without shame |
| B3 | Gawrilow, C., & Gollwitzer, P. M. (2008). Implementation intentions facilitate response inhibition in children with ADHD. *Cognitive Therapy and Research*, 32, 261–280. [doi:10.1007/s10608-007-9150-1](https://doi.org/10.1007/s10608-007-9150-1) (full text) | Study 1: 30 boys with ADHD + 28 controls, ages 8–14 | Clinic-diagnosed ADHD (boys only) | Go/No-Go inhibition with if-then plan 89.9% vs 78.3% with goal only, t(28) = 4.05, p < .001; reached control-group level. Plan wording: "And if I hear a tone, then I will not press any key!" Study 2: plans + medication best | Single session | — | **High: the only ADHD-specific evidence that a self-made if-then plan helps in the moment.** Lab task, not phones |
| B4 | Gollwitzer, P. M., & Sheeran, P. (2006). Implementation intentions and goal achievement: a meta-analysis. *Adv Exp Soc Psychol*, 38, 69–119. [doi:10.1016/S0065-2601(06)38002-1](https://doi.org/10.1016/S0065-2601(06)38002-1) (secondary: full text not opened; figure as quoted from the abstract) | 94 tests | Mixed | d = .65 on goal attainment | Varies | — | Medium: general support for if-then plans |
| B5 | Elliott, M. A., et al. (2021). Evidence that implementation intentions reduce drivers' use of mobile phones while driving. *Transp Res F*, 78, 381–397. [doi:10.1016/j.trf.2021.03.002](https://doi.org/10.1016/j.trf.2021.03.002) (abstract) | 136 | UK drivers, RCT | Less self-reported phone use while driving, **only in the situations they had planned for**; no change elsewhere | 1 month | — | High: if-then plans about phones work for the cue you name, so tie each plan to a specific app |
| B6 | DeStasio, K. L., Hill, A., & Berkman, E. T. (2016). Efficacy of an SMS-based smoking intervention using message self-authorship: a pilot study. PsyArXiv. [doi:10.31234/osf.io/3u3t2](https://doi.org/10.31234/osf.io/3u3t2) (lab summary at [sanlab.uoregon.edu](https://sanlab.uoregon.edu/?p=684)) | Not stated | Smokers, 1 month, 6 texts/day | Receiving one's own messages vs generic made **no large difference**; if-then format hinted at a benefit, not significant | 1 month | — | Medium: **self-authorship alone is not proven**; the act of writing may matter more |
| B7 | Lyngs, U., Lukoff, K., et al. (2020). "I just want to hack myself to not get distracted": evaluating design interventions for self-control on Facebook. *CHI '20*. [doi:10.1145/3313831.3376672](https://doi.org/10.1145/3313831.3376672) (abstract) | 58 | University students, 6 weeks | Goal reminders and removing the newsfeed both helped people stay on task | Mid-study weeks only | **Goal reminders were often irritating**; no-feed users feared missing information | High: reminders work and annoy, so make them rare and editable |
| B8 | Meinhardt, L.-M., Elhaidary, M., Colley, M., Rietzler, M., Rixen, J. O., Purohit, A. K., & Rukzio, E. (2025). Scrolling in the deep: analysing contextual influences on intervention effectiveness during infinite scrolling on social media. *CHI '25*. [arXiv:2501.11814](https://arxiv.org/abs/2501.11814) (abstract) | 72 | 7-day field study, Android, six platforms including TikTok and Shorts | Being at home, sleepiness and mood affected response; **sleepiness lowered reactance**; low mood at home slowed responses | 7 days | Reactance measured | High for reels detector |
| B9 | Meinhardt, L.-M., Dragic, M., Colley, M., Lukoff, K., & Rukzio, E. (2026). Can't stop: how context and individual traits influence effectiveness of different gradual interventions for infinite scrolling on short-form video platforms. *Proc. ACM IMWUT*. [arXiv:2607.15818](https://arxiv.org/abs/2607.15818) (full text read in part) | 104 | 7-day within-subject field study | Triggered after **15 min** of continuous scrolling. The explicit pop-up ("Time to close Instagram") gave the fastest disengagement but **"quickly lost influence if ignored"** (rating tipping point ≈ 62 s). **High-impulsivity / low self-control users did best with the explicit pop-up**; subtle friction was easy to adapt to | 7 days only | Haptic friction raised reactance over time | **High: closest study to the reels detector and to impulsive users.** Detection used Android's AccessibilityService |
| B10 | Kim, K., Kim, M., Jeong, S., So, J., & Song, H. (2026). Seeing your mindless face: how viewing one's live self interrupts mindless short-form video scrolling. *CHI EA '26*. [arXiv:2604.19424](https://arxiv.org/abs/2604.19424) (abstract) | 84 | Lab, YouTube Shorts | Self-related cues (live camera, selfie, own name) disrupted mindless viewing and led to voluntary stopping; the black-screen control produced the most intention to keep using the app; interviewees preferred subtler cues | Single session | Discomfort with live camera | Medium: "about you" cues help; camera is out of scope for Glitch |
| B11 | Orzikulova, A., et al. (2024). Time2Stop: adaptive and explainable human-AI loop for smartphone overuse intervention. *CHI '24*. [doi:10.1145/3613904.3642747](https://doi.org/10.1145/3613904.3642747) ([arXiv](https://arxiv.org/abs/2403.05584)) (abstract) | 71 | 8-week field study | Adaptive timing beat fixed baselines on accuracy (>32.8%) and receptivity (>8.0%); explanations added more; app visits −7.0% to −8.9% | 8 weeks | Not reported | Medium: timing matters; ML and usage tracking are out of scope |
| B12 | Anderson, B. B., Kirwan, C. B., Jenkins, J. L., Eargle, D., Howard, S., & Vance, A. (2015). How polymorphic warnings reduce habituation in the brain: insights from an fMRI study. *CHI '15*, 2883–2892. [doi:10.1145/2702123.2702322](https://doi.org/10.1145/2702123.2702322) ([BYU record](https://scholarsarchive.byu.edu/facpub/9306)) (abstract) | Not in record | Lab (fMRI + mouse tracking) | Visual processing of a warning drops sharply **after the second exposure**; a warning that changes its appearance resisted habituation in both experiments | Lab; the group's later work reports it holding over a week | — | High: vary the reminder's look and wording |
| B13 | Okeke et al. (2018), R#16 | 50 | MTurk, 3 weeks | Over-limit vibration cut use >20% | **Did not persist** after removal | — | Medium: reminders work only while present |
| B14 | Kim, Y.-H., Jeon, J. H., Choe, E. K., Lee, B., Kim, K., & Seo, J. (2016). TimeAware: leveraging framing effects to enhance personal productivity. *CHI '16*. [doi:10.1145/2858036.2858428](https://doi.org/10.1145/2858036.2858428) ([PDF](https://terpconnect.umd.edu/~choe/download/CHI-2016-Kim-TimeAware.pdf)) (abstract) | 24 | 8-week deployment, desktop | Only the version that highlighted **distracting** time improved productivity; an ambient widget kept people engaged | 8 weeks | — | Medium: show distraction time factually. This is framing of *data*, not blaming the person |
| B15 | Biwer, F., Wiradhany, W., oude Egbrink, M. G. A., & de Bruin, A. B. H. (2023). Understanding effort regulation: comparing 'Pomodoro' breaks and self-regulated breaks. *Br J Educ Psychol*, 93(2), 353–367. [PubMed 36859717](https://pubmed.ncbi.nlm.nih.gov/36859717/) (abstract) | 87 | Dutch university students, 1 day | Fixed breaks (24/6 or 12/3 min) gave better mood and similar task completion in less time vs self-chosen breaks | 1 day | — | Low/Medium: no ADHD data; conflicts with B19 |
| B16 | Eagle, T., Baltaxe-Admony, L. B., & Ringland, K. E. (2024). "It was something I naturally found worked and heard about later": an investigation of body doubling with neurodivergent participants. *ACM TACCESS*, 17(3), 16. [doi:10.1145/3689648](https://doi.org/10.1145/3689648) (abstract) | 220 survey | Mostly neurodivergent (ADHD, autism) | Describes body doubling for starting, staying on and finishing tasks; many used it for years before knowing the name | None (survey) | — | Low: popular, but **no controlled trial**; would need networking |
| B17 | Silverman, J., & Barasch, A. (2023). On or off track: how (broken) streaks affect consumer decisions. *J Consumer Research*, 49(6), 1095–1117. [doi:10.1093/jcr/ucac029](https://doi.org/10.1093/jcr/ucac029) (secondary: article in *Psychology Today*) | Multiple experiments | Online participants | Intact streak → ~66% chose the target behavior vs ~58% after a break; streaks raise engagement while intact and **lower it below no-tracking once broken** | Short lab streaks | Disengagement after a break | High: **do not add streaks** |
| B18 | Breines, J. G., & Chen, S. (2012). Self-compassion increases self-improvement motivation. *PSPB*, 38(9), 1133–1143. [doi:10.1177/0146167212445599](https://doi.org/10.1177/0146167212445599) (abstract) | 4 experiments (n not in abstract) | Students | Self-compassion after a failure led to more motivation to improve and more study effort than self-esteem or control conditions | Short | — | Medium: supports kind copy after a slip |
| B19 | Hariadi, M. C., Chow, K., & McGrenere, J. (2026). Advancing inclusive digital well-being tools: how neurodivergent students use distraction blockers. *CHI '26*. [doi:10.1145/3772318.3790801](https://doi.org/10.1145/3772318.3790801) (abstract + detailed review) | 27 interviews | Students with ADHD, autism and/or GAD (17 diagnosed) | Students **ignored or reset timers when in flow**, dropped Pomodoro routines, deliberately paused blockers for "digital stimming", valued hyperfocus; blockers could "amplify shame". Suggests completion-based rules, positive framing, brief check-ins, optional body doubling | Qualitative | Shame, stress from timers | **High: the only study of how neurodivergent people actually use these tools** |
| B20 | Kim, J., Jung, H., Ko, M., & Lee, U. (2019). GoalKeeper, R#14 | 36 | 4 weeks | Lockouts beat warnings | — | **More frustration and pressure** | Medium: why we avoid hard locks |
| B21 | Kasturiratna et al. (2025), R#13 | 163 | Crossover | Friction bundle cut screen time and checking | — | **More stress** | Medium |
| B22 | Monge Roffarello, A., & De Russis, L. (2023). Achieving digital wellbeing through digital self-control tools: a systematic review and meta-analysis. *ACM TOCHI*, 30(4). [doi:10.1145/3571810](https://doi.org/10.1145/3571810) (abstract) | Review | — | Pooled effect estimated but not stated in the abstract; calls for long-term studies and moving beyond overuse and self-monitoring | — | — | Low: the field is young |
| B23 | Lukoff, K., Lyngs, U., Zade, H., et al. (2021). How the design of YouTube influences user sense of agency. *CHI '21*. [doi:10.1145/3411764.3445467](https://doi.org/10.1145/3411764.3445467) ([arXiv](https://arxiv.org/abs/2101.11778)) (abstract) | 120 survey + 13 co-design | US YouTube app users | **Autoplay and recommendations lowered sense of agency; search and playlists supported it**; goal-directed users wanted more control | — | — | High: Glitch is "search-like" (you pick). Don't add feeds or suggestions |
| B24 | Monge Roffarello, A., Lukoff, K., & De Russis, L. (2023). Defining and identifying attention capture deceptive designs in digital interfaces. *CHI '23*. [doi:10.1145/3544548.3580729](https://doi.org/10.1145/3544548.3580729) (authors' slides) | 43 papers reviewed | — | 11 attention-capture damaging patterns, incl. Infinite Scroll, Neverending Autoplay, Casino Pull-to-Refresh, Time Fog, Fake Social Notifications, Guilty-Pleasure Recommendations | — | — | High: a checklist of what Glitch must never do |
| B25 | Rixen, J. O., et al. (2023). The loop and reasons to break it: investigating infinite scrolling behaviour in social media applications and reasons to stop. *Proc. ACM HCI* 7(MHCI), 228. [doi:10.1145/3604275](https://doi.org/10.1145/3604275) (abstract) | 46 | 1-week field study | Infinite scroll makes users feel caught in a loop that lengthens sessions they regret; catalogued reasons people stop | 1 week | — | Medium: context for the reels detector |

Grayscale (R#10, R#11), home-screen rearrangement bundles (R#12, R#13) and menu-order costs (R#5, R#6) are covered in the earlier review. None of them was tested in people with ADHD.

### 2c. Guidance and regulation (opened)

| # | Source | What it says that matters |
|---|---|---|
| G1 | W3C (2021). *Making Content Usable for People with Cognitive and Learning Disabilities*, W3C Working Group Note, 29 April 2021. [w3.org/TR/coga-usable](https://www.w3.org/TR/coga-usable/) | 8 objectives, including "Help Users Focus" ("Limit Interruptions", "Avoid Too Much Content"), "Help Users Avoid Mistakes" ("Let Users Go Back", "Avoid Data Loss and Timeouts"), "Use Clear and Understandable Content" ("Use Clear Words", "Keep Text Succinct"), "Ensure Processes Do Not Rely on Memory", "Provide Reminders", and "Support Adaptation and Personalization". **Tension:** COGA also recommends "Use a Consistent Visual Design" and a familiar layout. Glitch's shuffle deliberately breaks this. For some users with cognitive disabilities that is a cost, not a feature. |
| G2 | FDA (6 Jan 2026). *General Wellness: Policy for Low Risk Devices* (supersedes the 2019 version). [fda.gov/media/90652/download](https://www.fda.gov/media/90652/download) | General-wellness claims may include "improve mental acuity, instruction following, concentration…" as long as they **do not refer to a disease or condition**. Examples it lists as *not* wellness: "A claim that a product helps treat an anxiety disorder" and "A claim that a computer game will diagnose or treat autism". |
| G3 | FDA De Novo DEN200026, EndeavorRx (Akili), decision 15 June 2020. [Database entry](https://www.accessdata.fda.gov/scripts/cdrh/cfdocs/cfpmn/denovo.cfm?id=DEN200026), [decision summary](https://www.accessdata.fda.gov/cdrh_docs/reviews/DEN200026.pdf) | New Class II device type (21 CFR 882.5803): "software intended to provide therapy for ADHD or any of its individual symptoms as an adjunct to clinician supervised treatment". Prescription only, ages 8–12, indicated "to improve attention function as measured by computer-based testing", and "may not display benefits in typical behavioral symptoms". Pivotal RCT: TOVA API +0.93 vs +0.03 for control (p = .006); 6.7% treatment-related adverse events (decreased frustration tolerance, headache, emotional reaction). |
| G4 | Google Play, *Health Content and Services* policy. [support.google.com/googleplay/android-developer/answer/16679511](https://support.google.com/googleplay/android-developer/answer/16679511?hl=en) | Bans "misleading health claims that contradict existing medical consensus" and misleading or harmful medical functionality. Health/medical apps that are not regulated devices must state in the description that the app is "not a medical device and does not diagnose, treat, cure, or prevent any medical condition", and must tell users to consult a professional. Regulated devices must show proof of clearance. Health-app categories include "Mental and Behavioral Health" ([categories](https://support.google.com/googleplay/android-developer/answer/13996367)). |
| G5 | Google Play, *Use of the AccessibilityService API*. [support.google.com/googleplay/android-developer/answer/10964491](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en) | Non-accessibility tools may use the API, but need an in-app prominent disclosure with affirmative consent (separate from other disclosures, not just in the privacy policy) and a Play Console accessibility declaration. "Monitoring apps", "automation tools" and "launchers" are named as not being accessibility tools. |

---

## 3. What we did not find

- No randomized trial of any phone self-control tool (friction, blocker, reminder, grayscale, timer) **in people with ADHD**. ADHD responses are inferred from trait studies (A6, A7), lab work (B3), interviews (B19) and one impulsivity-moderation analysis (B9).
- No meta-analysis pooling ADHD symptoms with problematic *smartphone* use specifically.
- No controlled trial of body doubling, or of visible timers for adults with ADHD.
- No study of a shuffled app menu (unchanged from the earlier review).
- No trial comparing self-written with generic reminders for phone use over months. The one small pilot we found (B6) showed no large difference.

---

## 4. Design principles for Glitch

1. **Always offer an easy way out, and make "don't open" the easiest option.** The dismiss option was the strongest part of one sec (B1), and hard lockouts frustrate (B20) or raise stress (B21). ADHD students reset or disable rigid tools (B19). Any pause gets a big "Close" button and a smaller, always-available "Open anyway". Never lock.
2. **A choice beats a lecture.** A reflective message alone had no effect (B1), and goal reminders irritated (B7). Every prompt ends in a decision, not a paragraph.
3. **Make time visible and factual.** ADHD time perception is less accurate (A7), and hiding time is a known attention-capture pattern ("Time Fog", B24). Showing distraction time can change behavior (B14). Use plain numbers ("14 min"), with no judgment attached.
4. **Be explicit, not sneaky.** Impulsive users responded best to an explicit pop-up, and subtle friction was easy to tune out (B9). Prefer honest, clearly labelled prompts over hidden delays or tricks.
5. **Vary the form so it keeps being seen.** Attention to a repeated warning drops after the second exposure, and changing its appearance resists this (B12). Pop-ups lose force (B9) and dismissal rates plateau (B1). Rotate wording and layout. The shuffle is already a polymorphic design.
6. **Kind about the person, factual about the behavior.** Tools can amplify shame (B19), self-compassion supports trying again (B18), and broken streaks push people away (B17). No streaks, no failure counts, no "again" or "wasted". Facts about time are fine (B14). Judgments about the user are not.
7. **Let users write their own if-then plans, tied to a specific app.** If-then plans brought children with ADHD to control-group inhibition (B3), and phone plans worked only for the named situation (B5). Self-authorship alone is unproven (B6), so the *plan* format matters more than personalization.
8. **Flexible, reversible, under the user's control.** Respect hyperfocus and deliberate "digital stimming" (B19). Offer "pause for today" with no penalty. Pausing is normal and relapse is quick (B2), so make resuming effortless. COGA: support personalization and let users go back (G1).
9. **Low cognitive load.** Short text, at most two or three choices, large targets, one screen (G1 "Keep Text Succinct", "Avoid Too Much Content"). Nothing to remember: show the user's own plan, don't ask them to recall it (G1 "Do Not Rely on Memory"; A12 shows intentions get lost).
10. **Glitch must never become an interrupter.** Notifications raise inattention (A8) and disrupt attention even unanswered (A9). Glitch should only speak when the user opens something, never push. If notifications are ever added, batch them, and never use "no notifications at all" as the goal (A11: FoMO).
11. **No feed, no autoplay, no recommendations, no "suggested apps".** These lower sense of agency (B23) and appear in the damaging-pattern list (B24). Glitch stays a search-and-choose tool.
12. **Privacy is part of trust.** Keep everything on the device. Any feature that needs AccessibilityService or usage access changes the product's promise and must go through the owner and Play's disclosure rules (G5).

---

## 5. Candidate features, ranked

Ranking weighs evidence, fit with ADHD findings, and fit with the current no-permission design. **Evidence strength**: strong = several RCTs or meta-analyses on this mechanism for phones; moderate = at least one controlled field/online experiment on a closely related mechanism; weak = indirect, lab-only, qualitative or trait-based; none = nothing found.

| Rank | Feature | What it is | Evidence | Main risk | Needs scope / permission change? |
|---|---|---|---|---|---|
| 1 | **"Still want this?" pause for chosen apps** | When the user taps an app *they* marked as a time-sink inside Glitch, show one screen: app name, a big **Close** and a smaller **Open anyway**. No forced wait by default. | **Moderate** (B1 online RCT: dismiss option strongest; B1/B2 field data; not ADHD-tested; developer co-authors) | Only works when the app is opened *from Glitch*; easy to bypass via home screen. Annoyance (B7). | Scope only (prompts are a non-goal today). No permission. |
| 2 | **User-written if-then plans on the pause** | Per app, the user writes 1–5 short plans: "If I open Instagram, then I first reply to Sam." One is shown, rotating. | **Moderate** for if-then plans (B3 ADHD lab, B4 meta, B5 phone RCT); **weak** for showing them in a phone pause | Writing takes effort (ADHD start-up cost); plans go stale. Offer 3 editable starters. | Scope only. Stored locally. |
| 3 | **Kind-by-default copy and no streaks** | Copy rules across the app: no streaks, scores, failure counts or shaming words; "pause for today" without guilt. | **Moderate** (B17 streak experiments, B18, B19 qualitative) | Very low. Copy can feel bland; test it with users. | No. |
| 4 | **"Away time" card on return** | When Glitch comes back to the foreground, show a quiet line: "You left Glitch 23 min ago to open YouTube." Glitch already knows when it stopped and which app it launched. | **Weak** (A7 time perception, B14 framing, B24 Time Fog; never tested in this form) | Can read as surveillance or blame. Must be factual, dismissible, and possible to turn off. Measures time *away from Glitch*, not time in the app. | Scope only. No permission; stays on device. |
| 5 | **Varied pause presentation** | Rotate the plan text, layout, accent color and button position (Close stays easy to hit) so the pause doesn't become wallpaper. | **Moderate** for habituation to repeated warnings (B12, lab); **weak** for this use | Unpredictability can stress some users (G1 consistency). Keep a "calm, consistent" option. | No (part of 1). |
| 6 | **Reels detector with the user's own reminder** | Detect continuous short-form scrolling and show the user's reminder after N minutes (see section 6). | **Moderate** for initial effect of an explicit pop-up in impulsive users (B9, 7 days); **weak** for lasting effect (B9, B13) | **Needs AccessibilityService**: breaks AGENTS.md privacy rules, needs Play declaration and prominent disclosure (G5), and reads screen content. Big trust and review risk. | Yes: permissions, privacy policy, Data safety, owner approval. |
| 7 | **Optional short delay before "Open anyway"** | A user setting: 3–5 s before "Open anyway" activates. "Close" is never delayed. | **Moderate** (B1: delay helped, less than dismiss) | Reactance, rage-tapping; ADHD users may uninstall. Off by default. | Scope only. |
| 8 | **Notification-batching helper** | A screen that explains batching and links to Android's own notification settings; Glitch never reads notifications. | **Moderate** (A11 one RCT; A8) | Little. Out of Glitch's "app menu" purpose. | Scope only. |
| 9 | **Pinned essentials / gentler shuffle** | Let users pin 2–4 apps (phone, maps, authenticator) in fixed spots while the rest shuffles. | **Weak** (G1 consistency; R#5/R#6 show fixed order is faster) | Weakens the core mechanism. Users may pin their time-sinks. | Changes the core behavior; owner call. |
| 10 | **Flexible focus window** | Optional "I'm working on X until I'm done" mode that adds the pause to more apps, ended by the user (completion-based, not clock-based). | **Weak** (B15 has no ADHD data; B19 says fixed timers clash and suggests completion-based rules) | Becomes a blocker; risk of shame when abandoned. | Scope. |
| 11 | **Body doubling / co-focus with others** | Join others virtually while working. | **Weak** (B16 survey only) | Needs network, accounts and a backend: breaks every privacy promise. | Yes. Not recommended. |
| 12 | **Streaks, points, rewards** | Gamify days "without" an app. | **None** for benefit here; **moderate evidence of harm** when broken (B17) | Shame and quitting after the first break. | Not recommended. |

**Not recommended at all:** hard lockouts and un-dismissable blockers (B20, B21, B19), autoplay or suggestion feeds (B23, B24), and anything that pushes notifications to "bring the user back".

---

## 6. Reels detector: evidence and the best reminder

### 6.1 Short-form video, attention and ADHD

- **Association.** Across 71 studies (98,299 people), heavier short-form video use correlated with poorer attention (r = −.38) and inhibitory control (r = −.41), weaker with mental health (r = −.21) (A13). The data are correlational and mostly self-reported, so the direction is unknown. We found no ADHD-specific meta-analysis of short-form video.
- **Mechanism, experimental.** TikTok viewing, unlike Twitter or YouTube, impaired carrying out a planned action (A12). This is the "I'll stop after this one" failure the detector targets.
- **Design mechanisms.** Infinite scroll, neverending autoplay and "casino pull-to-refresh" exploit variable reward and remove stopping points (B24). Autoplay and recommendations reduce sense of agency (B23). Infinite scroll produces regretted, extended sessions (B25). The ADHD traits in A6 (now beats later) and A7 (time slips) make each of these stronger.

### 6.2 What works and what backfires in interruptions

| Question | What the evidence says | Strength |
|---|---|---|
| **Self-authored messages** | The one sec authors blame their generic message's null effect on it being generic and suggest personalization (B1), but this was not tested. Self-related cues (own name, own face) disrupted Shorts viewing in the lab (B10). In a smoking pilot, receiving one's *own* messages vs generic ones made no large difference (B6). | Weak. Promising, unproven. |
| **Implementation intentions** ("If I open Reels, then I…") | Strong general effect (B4). In children with ADHD, if-then plans closed the inhibition gap in the lab (B3). For phones they worked **only for the situation named** (B5). | Moderate. Best-supported content for the reminder. |
| **Future-self / values reminders** | No study we opened tested values or future-self prompts against phone scrolling. Goal reminders on Facebook helped but irritated (B7). | Weak / none. |
| **Personalized vs generic** | Generic message: no effect (B1). Adaptive, explained timing improved receptivity (B11). Self-related cues worked in the lab (B10). | Weak to moderate. |
| **Timing: immediate vs after N minutes** | At app open, a choice point is the strongest lever (B1). During scrolling, an explicit pop-up after 15 min gave the fastest disengagement, but its pull faded within about a minute if ignored (B9). Context (home, sleepy, low mood) changes response (B8). | Moderate for "at open"; moderate for "after N min, act on it right away". |
| **Tone: kind vs shaming** | Tools can amplify shame for neurodivergent users (B19). Self-compassion supports re-trying (B18). Broken-streak framing reduces engagement (B17). Factual framing of *distracting time* worked (B14). | Moderate. Factual and kind; no blame. |
| **Habituation and rotation** | Attention to a repeated warning drops after the second view, and varied appearance resists it (B12). Pop-up effect fades (B9). one sec's dismissal rate fell from 43% to a 32–34% plateau, then held (B1). Effects stop when the tool stops (B13). | Moderate (lab) for varying form; no data on how many variants are enough. |
| **Explicit vs subtle** | High-impulsivity users did best with the explicit pop-up; subtle friction was adapted to (B9). | Moderate (one 7-day study). |
| **one sec's interruption data** | Dismiss option > delay > message (null). 36% of attempts closed overall; 37% fewer attempts by week 6; 57% fewer opens. 61% of starters dropped out (B1). | Moderate, with conflicts of interest and self-selection. |

### 6.3 Recommendation: "the best reminder"

Every rule below is tied to the evidence. Treat it as a hypothesis to test, not a proven design.

**Wording template (two short lines + buttons):**

```
[Fact]      12 min of Reels.
[Own plan]  "If I'm still scrolling after 10 minutes, then I put the phone down and start the dishes."
            — written by you

[ Close Reels ]          ← large, primary, never delayed
[ 5 more minutes ]       ← snooze once per session
  Keep scrolling          ← plain text button, always available, no penalty
  Pause reminders today
```

- **Line 1 is a fact, not a verdict** (principle 3; B14, A7). No "you", "again", "wasted", "addicted", "should".
- **Line 2 is the user's own if-then plan for this app** (B3, B5). Setup asks: "When you've been on [app] longer than you wanted, what will you do instead?" and saves it as "If…, then I…". Offer three editable starters ("…then I stand up and drink water", "…then I open my to-do list", "…then I text a friend") so the start-up cost stays low.
- **Rotate** between the user's 3–5 plans and among 3–4 visual layouts (B12, B9). Keep the Close button large and in a predictable place, so variety never makes escape harder.
- **Choice, not lecture**: the screen is a decision point with Close as the default (B1).

**Timing:**
- **Default trigger: 10–15 minutes of continuous short-video scrolling**, user-adjustable from 5 to 30 (B9 used 15). Optionally also a light prompt at app open, which is where the choice point is strongest (B1).
- Show it **once**, explicitly. If the user picks "5 more minutes", show one follow-up. Don't repeat a prompt that was ignored, since repeats fade fast (B9) and irritate (B7).

**Frequency:**
- At most **one reminder plus one snooze follow-up per session**, and a **daily cap** (suggested 4) that the user sets.
- If the user taps "Keep scrolling" twice in a row in the same context, back off for that session (reactance; B8, B9).
- "Pause reminders today" is always one tap. Pausing is normal (B2, B19).

**Escape hatch:**
- **Close and Keep scrolling are both active immediately.** A short delay is an opt-in setting and only ever applies to "Keep scrolling" (B1: delay helps, but forced waits feed reactance and abandonment, B19–B21).
- No locks, no countdown, no "are you sure?" chains.

**What it must not do:** count failures, show streaks, send push notifications, read or store what the user watched, or report anything off the device.

**Platform reality check.** On Android, telling Reels or Shorts apart from the rest of Instagram or YouTube needs an **AccessibilityService** that reads the screen (the B8/B9 studies used one). Usage access (`PACKAGE_USAGE_STATS`) only tells which app is in front, not whether it's Reels. Both are banned by AGENTS.md today, and accessibility needs a Play declaration plus an in-app prominent disclosure (G5). A **no-permission alternative** that keeps today's privacy promise is features 1, 2 and 4: put the user's plan on the Glitch pause screen when they open a short-video app *from Glitch*, and show "away time" when they come back. It is weaker, since it only covers launches from Glitch, but it is honest and private.

---

## 7. Claims: allowed vs forbidden

The rules come from AGENTS.md (no focus/memory/intelligence claims), Google Play's Health Content and Services policy (G4), FDA's general-wellness guidance and ADHD device classification (G2, G3), and the absence of any study of Glitch itself. FDA's guidance would let a general-wellness product say "improve concentration" without naming a disease. That is still a *factual* claim that needs evidence we don't have, and AGENTS.md bans it, so it stays forbidden.

### Allowed (describes what the app does, or what the user chooses)

| Wording | Why it's OK |
|---|---|
| "Your apps in a new order every time, so you look before you tap." | Describes the app. |
| "Pick the apps you open on autopilot. Glitch asks 'Still want this?' before opening them, and Close is one tap." *(if built)* | Describes a feature. |
| "Write your own if-then plan. Glitch shows it back to you at the moment you open the app." *(if built)* | Describes a feature; no outcome promised. |
| "Shows how long you were away, in plain minutes. No scores, no streaks, no judgment." *(if built)* | Describes a feature. |
| "Designed with input from research on attention, habits and ADHD-friendly design (flexible, kind, easy to undo)." | True as design rationale. Keep it about the *design*, never the outcome. |
| "Built with neurodivergent users in mind." *(only if we actually test with them)* | Describes our process. |
| "No permissions, no internet, no tracking. Nothing leaves your phone." | Describes the app (today's build). |
| "Glitch is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. For ADHD care, talk to a healthcare professional." | Play's required disclaimer wording (G4) if Play ever treats the app as health-related. Safe to include anyway. |

### Forbidden

| Wording | Why not |
|---|---|
| "Helps with ADHD", "ADHD app", "for ADHD treatment", "manages ADHD symptoms", "an alternative to medication" | Software intended to provide therapy for ADHD or any of its symptoms is a Class II medical device (G3). Implies equivalence to treatment (G4). |
| "Reduces inattention / impulsivity / hyperactivity", "improves executive function", "fixes time blindness" | These are ADHD symptoms. Making a claim about them is a device claim (G3), and it is untested for Glitch. |
| "Improves focus / attention / concentration / productivity / memory" | Banned by AGENTS.md. No study of Glitch. FDA leniency on wellness wording does not create evidence. |
| "Clinically proven", "science-backed", "research shows Glitch works", "doctor-recommended" | No direct study exists. |
| "Reduces screen time by X%", "cuts Reels time", "57% fewer opens", or quoting B1/B9 numbers next to Glitch | Borrowed numbers from other tools and designs; misleading. |
| "Breaks your phone addiction", "cures doomscrolling", "dopamine detox", "rewires your brain" | Addiction/medical framing and neuroscience claims with no support. |
| "Like EndeavorRx but free", "FDA-approved", "FDA-cleared" | False, and invites device regulation. |
| "Stops brain rot", "TikTok damages your attention" (as fact) | A13 is correlational; causation not shown. |
| Words that shame: "Caught you", "Again?", "You wasted 2 hours" | Not a legal claim, but contradicts the evidence on shame (B17–B19) and our principle 6. |

**Safe pattern for any future store copy:** *[what Glitch does] + [the choice stays yours]*, e.g. "Glitch puts a short pause and your own plan between you and the apps you pick. You decide what happens next." No outcome verbs (improve, reduce, help, treat, boost) next to attention, focus, ADHD, or time.

---

## Caveats

- Most intervention studies are short (1 day to 8 weeks), use students or self-selected users, and rely partly on self-report. None follows ADHD adults.
- Several key numbers (B4 effect size, B17 percentages, EndeavorRx press details) come from secondary reporting or abstracts, as marked. G3's numbers come from the FDA decision summary itself.
- Two friction studies (B1, B2) include the tested app's developer. B9 and B10 are 2026 papers (journal/EA versions) and have not been replicated.
- This review supports design choices. It does not support any claim that Glitch changes behavior, attention or ADHD symptoms. That would need our own study.
