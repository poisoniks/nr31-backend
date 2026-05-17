UPDATE app_config
SET config_value = '{
    "hero": ["hero"],
    "sidebar": ["nextevent", "discord", "youtube"],
    "content": ["richtext", "newsfeed"]
}'
WHERE config_key = 'cms_slot_restrictions';

INSERT INTO pages (slug, title, version)
VALUES ('home', '{"en": "Home", "uk": "Головна"}'::jsonb, 1);

INSERT INTO page_revisions (page_id, status, layout_data)
SELECT id, 'PUBLISHED', '{
  "slots": [
    {
      "slotType": "hero",
      "widgets": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440001",
          "type": "hero",
          "badgeText": {
            "en": "Part of the USC Community",
            "uk": "Частина ЄУК спільноти"
          },
          "titleMain": "Nr.31",
          "titleSub": "Feldkanonenregiment",
          "description": {
            "en": "Choose your path: infantry, jaegers, or artillery. Join the ranks of the Austrian regiment in Mount & Blade: Napoleonic Wars.",
            "uk": "Обирай свій шлях: піхота, єгері чи артилерія. Ставай до лав українськомовного полку в Mount & Blade: Napoleonic Wars."
          },
          "ctaText": {
            "en": "Join the Regiment",
            "uk": "Приєднатися до полку"
          },
          "ctaTargetId": "how-to-join",
          "backgroundImageId": null
        }
      ]
    },
    {
      "slotType": "content",
      "widgets": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440002",
          "type": "richtext",
          "bodyContent": {
            "en": {
              "type": "doc",
              "content": [
                {
                  "type": "heading",
                  "attrs": {"level": 2},
                  "content": [{"type": "text", "text": "About Us"}]
                },
                {
                  "type": "paragraph",
                  "content": [
                    {
                      "type": "text",
                      "text": "We are a community of veteran players and enthusiasts of the Napoleonic Wars. Our regiment reconstructs the Austrian Feldkanonenregiment Nr. 31 from Stanislau. We participate in top-tier European events, ranging from Flagspawn and historical battles to competitive LB and GF formats."
                    }
                  ]
                },
                {
                  "type": "heading",
                  "attrs": {"level": 3},
                  "content": [{"type": "text", "text": "What can we offer?"}]
                },
                {
                  "type": "bulletList",
                  "content": [
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "A true Ukrainian community"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Communication only in Ukrainian"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "10-20 active players at events"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "High number of quality European events"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Opportunity to play as infantry, artillery, sapper, doctor, jaeger"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Pleasant discussions on various topics among reasonable people"}]}]}
                  ]
                },
                {
                  "type": "heading",
                  "attrs": {"level": 3},
                  "content": [{"type": "text", "text": "Regiment Command"}]
                },
                {
                  "type": "bulletList",
                  "content": [
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Regiment Leader - Manul"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Officer - Dizhka"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Officer - GodyX"}]}]}
                  ]
                },
                {
                  "type": "heading",
                  "attrs": {"level": 3},
                  "content": [{"type": "text", "text": "Important links"}]
                },
                {
                  "type": "paragraph",
                  "content": [
                    {
                      "type": "smallLinkButton",
                      "attrs": {
                        "href": "https://discord.gg/uuc",
                        "label": "EUC",
                        "target": "_blank",
                        "bgColor": null,
                        "textColor": null
                      }
                    },
                    {"type": "text", "text": " "},
                    {
                      "type": "smallLinkButton",
                      "attrs": {
                        "href": "https://www.fsegames.eu/forum/index.php?topic=49585.0",
                        "label": "Forum thread",
                        "target": "_blank",
                        "bgColor": null,
                        "textColor": null
                      }
                    },
                    {"type": "text", "text": " "},
                    {
                      "type": "smallLinkButton",
                      "attrs": {
                        "href": "https://docs.google.com/spreadsheets/d/1kkzzUGL1VGYnH3OrzGC8TsalzWd42ZNYfP6yhvxPi7U/edit?gid=0#gid=0",
                        "label": "Regiment roster",
                        "target": "_blank",
                        "bgColor": null,
                        "textColor": null
                      }
                    },
                    {"type": "text", "text": " "},
                    {
                      "type": "smallLinkButton",
                      "attrs": {
                        "href": "https://www.youtube.com/@silentfanat2698",
                        "label": "YouTube",
                        "target": "_blank",
                        "bgColor": null,
                        "textColor": null
                      }
                    }
                  ]
                }
              ]
            },
            "uk": {
              "type": "doc",
              "content": [
                {
                  "type": "heading",
                  "attrs": {"level": 2},
                  "content": [{"type": "text", "text": "Про нас"}]
                },
                {
                  "type": "paragraph",
                  "content": [
                    {
                      "type": "text",
                      "text": "Ми — спільнота досвідчених гравців та ентузіастів світу Napoleonic Wars. Наш полк реконструює австрійський Feldkanonenregiment Nr. 31 зі Станіслау. Ми беремо участь у якісних європейських івентах: від Flagspawn та історичних битв до змагальних форматів ЛБ та ГФ."
                    }
                  ]
                },
                {
                  "type": "heading",
                  "attrs": {"level": 3},
                  "content": [{"type": "text", "text": "Що ми можемо запропонувати?"}]
                },
                {
                  "type": "bulletList",
                  "content": [
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Справжнє українське ком''юніті"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Спілкування лише українською"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "10-20 людей активу на івентах"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Велика кількість якісних європейських івентів"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Можливість грати за піхоту, артилерію, сапера, лікаря, єгеря"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Приємні обговорення на будь-які теми серед адекватних людей"}]}]}
                  ]
                },
                {
                  "type": "heading",
                  "attrs": {"level": 3},
                  "content": [{"type": "text", "text": "Командування полку"}]
                },
                {
                  "type": "bulletList",
                  "content": [
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Лідер полку - Manul"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Офіцер - Dizhka"}]}]},
                    {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Офіцер - GodyX"}]}]}
                  ]
                },
                {
                  "type": "heading",
                  "attrs": {"level": 3},
                  "content": [{"type": "text", "text": "Важливі посилання"}]
                },
                {
                  "type": "paragraph",
                  "content": [
                    {
                      "type": "smallLinkButton",
                      "attrs": {
                        "href": "https://discord.gg/uuc",
                        "label": "ЄУК",
                        "target": "_blank",
                        "bgColor": null,
                        "textColor": null
                      }
                    },
                    {"type": "text", "text": " "},
                    {
                      "type": "smallLinkButton",
                      "attrs": {
                        "href": "https://www.fsegames.eu/forum/index.php?topic=49585.0",
                        "label": "Форумна сторінка",
                        "target": "_blank",
                        "bgColor": null,
                        "textColor": null
                      }
                    },
                    {"type": "text", "text": " "},
                    {
                      "type": "smallLinkButton",
                      "attrs": {
                        "href": "https://docs.google.com/spreadsheets/d/1kkzzUGL1VGYnH3OrzGC8TsalzWd42ZNYfP6yhvxPi7U/edit?gid=0#gid=0",
                        "label": "Полковий реєстр",
                        "target": "_blank",
                        "bgColor": null,
                        "textColor": null
                      }
                    },
                    {"type": "text", "text": " "},
                    {
                      "type": "smallLinkButton",
                      "attrs": {
                        "href": "https://www.youtube.com/@silentfanat2698",
                        "label": "YouTube",
                        "target": "_blank",
                        "bgColor": null,
                        "textColor": null
                      }
                    }
                  ]
                }
              ]
            }
          }
        },
        {
          "id": "550e8400-e29b-41d4-a716-446655440003",
          "type": "richtext",
          "bodyContent": {
            "en": {
              "type": "doc",
              "content": [
                {
                  "type": "heading",
                  "attrs": {"level": 2},
                  "content": [{"type": "text", "text": "How to join?"}]
                },
                {
                  "type": "bulletList",
                  "content": [
                    {
                      "type": "listItem",
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [{"type": "text", "text": "If you are Ukrainian and want to join us - write about this to any of our commanders:"}]
                        },
                        {
                          "type": "paragraph",
                          "content": [
                            {
                              "type": "smallLinkButton",
                              "attrs": {
                                "href": "https://steamcommunity.com/profiles/76561198250504840",
                                "label": "Manul",
                                "target": "_blank",
                                "bgColor": null,
                                "textColor": null
                              }
                            },
                            {"type": "text", "text": " "},
                            {
                              "type": "smallLinkButton",
                              "attrs": {
                                "href": "https://steamcommunity.com/profiles/76561198871671245",
                                "label": "Dizhka",
                                "target": "_blank",
                                "bgColor": null,
                                "textColor": null
                              }
                            },
                            {"type": "text", "text": " "},
                            {
                              "type": "smallLinkButton",
                              "attrs": {
                                "href": "https://steamcommunity.com/profiles/76561199466448146",
                                "label": "GodyX",
                                "target": "_blank",
                                "bgColor": null,
                                "textColor": null
                              }
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "type": "listItem",
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [
                            {"type": "text", "text": "You can also join the "},
                            {
                              "type": "text",
                              "marks": [
                                {
                                  "type": "link",
                                  "attrs": {
                                    "href": "https://discord.gg/uuc",
                                    "target": "_blank",
                                    "rel": "noopener noreferrer nofollow"
                                  }
                                },
                                {"type": "bold"}
                              ],
                              "text": "EUC server"
                            },
                            {"type": "text", "text": " and write \"I want to join the Nr31 regiment\" in chat, people will tell you what to do next. If no one answers - write a direct message in Discord to newton_manul."}
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            "uk": {
              "type": "doc",
              "content": [
                {
                  "type": "heading",
                  "attrs": {"level": 2},
                  "content": [{"type": "text", "text": "Як приєднатися?"}]
                },
                {
                  "type": "bulletList",
                  "content": [
                    {
                      "type": "listItem",
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [{"type": "text", "text": "Якщо Ви українець і хочете до нас - напишіть про це будь-кому з наших командувачів:"}]
                        },
                        {
                          "type": "paragraph",
                          "content": [
                            {
                              "type": "smallLinkButton",
                              "attrs": {
                                "href": "https://steamcommunity.com/profiles/76561198250504840",
                                "label": "Manul",
                                "target": "_blank",
                                "bgColor": null,
                                "textColor": null
                              }
                            },
                            {"type": "text", "text": " "},
                            {
                              "type": "smallLinkButton",
                              "attrs": {
                                "href": "https://steamcommunity.com/profiles/76561198871671245",
                                "label": "Dizhka",
                                "target": "_blank",
                                "bgColor": null,
                                "textColor": null
                              }
                            },
                            {"type": "text", "text": " "},
                            {
                              "type": "smallLinkButton",
                              "attrs": {
                                "href": "https://steamcommunity.com/profiles/76561199466448146",
                                "label": "GodyX",
                                "target": "_blank",
                                "bgColor": null,
                                "textColor": null
                              }
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "type": "listItem",
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [
                            {"type": "text", "text": "Також Ви можете приєднатися на сервер "},
                            {
                              "type": "text",
                              "marks": [
                                {
                                  "type": "link",
                                  "attrs": {
                                    "href": "https://discord.gg/uuc",
                                    "target": "_blank",
                                    "rel": "noopener noreferrer nofollow"
                                  }
                                },
                                {"type": "bold"}
                              ],
                              "text": "ЄУК"
                            },
                            {"type": "text", "text": " і у чат написати \"Я хочу у полк Nr31\", люди підкажуть згодом, що робити. Якщо Вам ніхто не відповідає - напишіть у ПП дискорда newton_manul."}
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          }
        },
        {
          "id": "550e8400-e29b-41d4-a716-446655440004",
          "type": "newsfeed",
          "sectionTitle": {"en": "Latest News", "uk": "Останні новини"},
          "itemCount": 2,
          "tagFilter": null
        },
        {
          "id": "550e8400-e29b-41d4-a716-446655440005",
          "type": "richtext",
          "bodyContent": {
            "en": {
              "type": "doc",
              "content": [
                {
                  "type": "heading",
                  "attrs": {"level": 2},
                  "content": [{"type": "text", "text": "Want to support us?"}]
                },
                {
                  "type": "imageLinkButton",
                  "attrs": {
                    "href": "https://send.monobank.ua/jar/3m9TAxi5Eb",
                    "label": "Monobank",
                    "imageUrl": "https://play-lh.googleusercontent.com/tVdBTQSX3ek05SxDZJClWtohEohC0EHLF7BRqzfq7tRsr3533ONjQxUd-pmQxjGtb2I=s48-rw",
                    "imageAlt": "Monobank",
                    "target": "_blank",
                    "bgColor": null,
                    "textColor": null
                  }
                },
                {
                  "type": "imageLinkButton",
                  "attrs": {
                    "href": "https://next.privat24.ua/send/27n07",
                    "label": "Privat24",
                    "imageUrl": "https://next.privat24.ua/assets/912a277127b20d16edea.svg",
                    "imageAlt": "Privat24",
                    "target": "_blank",
                    "bgColor": null,
                    "textColor": null
                  }
                }
              ]
            },
            "uk": {
              "type": "doc",
              "content": [
                {
                  "type": "heading",
                  "attrs": {"level": 2},
                  "content": [{"type": "text", "text": "Бажаєш підтримати нас?"}]
                },
                {
                  "type": "imageLinkButton",
                  "attrs": {
                    "href": "https://send.monobank.ua/jar/3m9TAxi5Eb",
                    "label": "Monobank",
                    "imageUrl": "https://play-lh.googleusercontent.com/tVdBTQSX3ek05SxDZJClWtohEohC0EHLF7BRqzfq7tRsr3533ONjQxUd-pmQxjGtb2I=s48-rw",
                    "imageAlt": "Monobank",
                    "target": "_blank",
                    "bgColor": null,
                    "textColor": null
                  }
                },
                {
                  "type": "imageLinkButton",
                  "attrs": {
                    "href": "https://next.privat24.ua/send/27n07",
                    "label": "Приват24",
                    "imageUrl": "https://next.privat24.ua/assets/912a277127b20d16edea.svg",
                    "imageAlt": "Privat24",
                    "target": "_blank",
                    "bgColor": null,
                    "textColor": null
                  }
                }
              ]
            }
          }
        }
      ]
    },
    {
      "slotType": "sidebar",
      "widgets": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440006",
          "type": "nextevent",
          "titleOverride": null
        },
        {
          "id": "550e8400-e29b-41d4-a716-446655440007",
          "type": "discord",
          "inviteCode": "uuc"
        },
        {
          "id": "550e8400-e29b-41d4-a716-446655440008",
          "type": "youtube",
          "channelId": "UCbU41G2hhiwdn-gFFRqZN4w"
        }
      ]
    }
  ]
}'::jsonb
FROM pages WHERE slug = 'home';
