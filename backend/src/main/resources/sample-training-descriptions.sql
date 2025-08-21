-- Sample TrainingDescription data for demonstration
-- These can be manually inserted into the database or loaded via a data initialization script

INSERT INTO training_descriptions (name, detailed_instructions, warmup_instructions, cooldown_instructions, equipment, tips, estimated_duration_minutes, difficulty_level) VALUES
('10 x 400m Intervals', 
 'Laufe 10 Wiederholungen von 400 Metern in einem schnellen Tempo (etwa 5K-Renngeschwindigkeit). Zwischen jeder Wiederholung machst du eine aktive Erholungspause von 90 Sekunden, indem du langsam gehst oder trabst.',
 '15 Minuten langsames Einlaufen, gefolgt von 4-6 Steigerungsläufen über 50-80 Meter',
 '10-15 Minuten lockeres Auslaufen mit anschließendem Stretching der Beinmuskulatur',
 'Laufschuhe, Sportuhr/Stoppuhr, evtl. Laufbahn oder gemessene 400m-Strecke',
 'Halte ein gleichmäßiges Tempo bei allen Wiederholungen. Die letzten 2-3 Intervalle sollten sich genauso anfühlen wie die ersten.',
 60,
 'high'),

('Long Steady Run',
 'Kontinuierlicher Lauf in einem angenehmen, gesprächsfähigen Tempo. Das Tempo sollte so gewählt sein, dass du während des gesamten Laufs problemlos sprechen könntest.',
 '5-10 Minuten sehr langsames Eingehen, dann allmählich auf Trainingstempo steigern',
 '5-10 Minuten langsames Auslaufen mit lockerem Stretching',
 'Laufschuhe, eventuell Trinkflasche bei längeren Läufen',
 'Konzentriere dich auf eine gleichmäßige Atmung und entspannte Körperhaltung. Bei Müdigkeit das Tempo leicht reduzieren.',
 90,
 'medium'),

('Hill Repeats',
 '6-8 Bergaufsprints auf einem Hügel mit 6-8% Steigung über 60-90 Sekunden. Laufe bergauf in einem harten, aber kontrollierten Tempo, dann gehe langsam bergab zur Erholung.',
 '15-20 Minuten Einlaufen auf flachem Terrain, dann 2-3 leichte Steigerungen',
 '10-15 Minuten lockeres Auslaufen auf flachem Terrain',
 'Laufschuhe mit gutem Grip, eventuell Trailschuhe je nach Untergrund',
 'Konzentriere dich auf kurze, schnelle Schritte bergauf. Nutze die Arme aktiv für zusätzlichen Antrieb.',
 50,
 'high'),

('Recovery Run',
 'Sehr lockerer, entspannter Lauf in langsamem Tempo. Das Ziel ist aktive Erholung und Förderung der Durchblutung in den Beinen.',
 '5 Minuten langsames Gehen, dann sanft ins Lauftempo übergehen',
 '5 Minuten Gehen mit leichtem Stretching',
 'Bequeme Laufschuhe',
 'Das Tempo sollte sehr komfortabel sein. Wenn du dich anstrengst, läufst du zu schnell.',
 30,
 'low'),

('Tempo Run',
 'Lauf in einem "angenehm harten" Tempo - schneller als dein normales Trainingstempo, aber nicht so hart wie bei einem 5K-Rennen. Das Tempo sollte für 20-30 Minuten durchhaltbar sein.',
 '15 Minuten progressives Einlaufen, von sehr langsam zu Trainingstempo',
 '10 Minuten lockeres Auslaufen',
 'Laufschuhe, Sportuhr für Tempokontrolle',
 'Das Tempo sollte sich "angenehm hart" anfühlen. Du solltest nicht völlig außer Atem sein, aber auch nicht entspannt sprechen können.',
 45,
 'medium');